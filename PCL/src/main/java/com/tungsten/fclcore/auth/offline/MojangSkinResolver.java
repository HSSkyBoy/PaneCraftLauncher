package com.tungsten.fclcore.auth.offline;

import static com.tungsten.fclcore.util.Logging.LOG;
import static java.nio.charset.StandardCharsets.UTF_8;

import android.graphics.BitmapFactory;

import com.google.gson.annotations.SerializedName;
import com.tungsten.fclcore.auth.yggdrasil.CompleteGameProfile;
import com.tungsten.fclcore.auth.yggdrasil.TextureModel;
import com.tungsten.fclcore.auth.yggdrasil.TextureType;
import com.tungsten.fclcore.auth.yggdrasil.YggdrasilService;
import com.tungsten.fclcore.util.Hex;
import com.tungsten.fclcore.util.StringUtils;
import com.tungsten.fclcore.util.gson.JsonUtils;
import com.tungsten.fclcore.util.io.IOUtils;
import com.tungsten.fclcore.util.io.NetworkUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Queries Mojang's public profile & session API by username so offline accounts
 * automatically use the same-name official account's skin, model, and cape by default.
 */
public final class MojangSkinResolver {

    private static final Pattern VALID_MC_USERNAME = Pattern.compile("^[A-Za-z0-9_]{1,16}$");
    private static final long HIT_TTL_MS = 30 * 60 * 1000L;
    private static final long NOT_FOUND_TTL_MS = 10 * 60 * 1000L;
    private static final long ERROR_TTL_MS = 30 * 1000L;

    private static volatile Path cacheDir;
    private static final ConcurrentHashMap<String, CacheEntry> PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Texture> TEXTURE_CACHE = new ConcurrentHashMap<>();

    private MojangSkinResolver() {
    }

    public static void setCacheDir(Path dir) {
        cacheDir = dir;
    }

    public static Optional<Skin.LoadedSkin> resolve(String username) {
        if (username == null || !VALID_MC_USERNAME.matcher(username).matches()) {
            return Optional.empty();
        }
        String key = username.toLowerCase(Locale.ROOT);
        ProfileMetadata meta = getOrFetchMetadata(key, username);
        if (meta == null || !meta.found) {
            return Optional.empty();
        }

        Texture skin = StringUtils.isNotBlank(meta.skinUrl) ? loadRemoteTexture(meta.skinUrl) : null;
        Texture cape = StringUtils.isNotBlank(meta.capeUrl) ? loadRemoteTexture(meta.capeUrl) : null;
        if (skin == null && cape == null) {
            return Optional.empty();
        }
        TextureModel model = "slim".equals(meta.model) ? TextureModel.ALEX : TextureModel.STEVE;
        return Optional.of(new Skin.LoadedSkin(model, skin, cape));
    }

    private static ProfileMetadata getOrFetchMetadata(String key, String username) {
        long now = System.currentTimeMillis();
        CacheEntry cached = PROFILE_CACHE.get(key);
        if (cached != null && now < cached.expiresAtMs) {
            return cached.metadata;
        }

        try {
            ProfileMetadata fetched = fetchFromMojang(username);
            long ttl = fetched.found ? HIT_TTL_MS : NOT_FOUND_TTL_MS;
            PROFILE_CACHE.put(key, new CacheEntry(fetched, now + ttl));
            if (fetched.found) {
                writeDiskMetadata(key, fetched);
            }
            return fetched;
        } catch (Exception e) {
            LOG.log(Level.FINE, "Failed to resolve Mojang profile for offline username " + username, e);
            ProfileMetadata disk = readDiskMetadata(key);
            if (disk != null && disk.found) {
                return disk;
            }
            PROFILE_CACHE.put(key, new CacheEntry(new ProfileMetadata(false, null, null, null), now + ERROR_TTL_MS));
            return null;
        }
    }

    private static ProfileMetadata fetchFromMojang(String username) throws Exception {
        URL profileLookupUrl = NetworkUtils.toURL(
                "https://api.mojang.com/users/profiles/minecraft/" + NetworkUtils.encodeLocation(username));
        HttpURLConnection lookupConn = NetworkUtils.resolveConnection(NetworkUtils.createHttpConnection(profileLookupUrl));
        int status = lookupConn.getResponseCode();
        if (status == HttpURLConnection.HTTP_NO_CONTENT || status == HttpURLConnection.HTTP_NOT_FOUND) {
            return new ProfileMetadata(false, null, null, null);
        }
        if (status != HttpURLConnection.HTTP_OK) {
            throw new IOException("Mojang profile lookup returned HTTP " + status);
        }
        String lookupBody;
        try (InputStream in = lookupConn.getInputStream()) {
            lookupBody = IOUtils.readFullyAsString(in);
        }
        if (StringUtils.isBlank(lookupBody)) {
            return new ProfileMetadata(false, null, null, null);
        }
        NameLookupResponse nameLookup = JsonUtils.GSON.fromJson(lookupBody, NameLookupResponse.class);
        if (nameLookup == null || StringUtils.isBlank(nameLookup.id)) {
            return new ProfileMetadata(false, null, null, null);
        }

        URL sessionUrl = NetworkUtils.toURL(
                "https://sessionserver.mojang.com/session/minecraft/profile/" + NetworkUtils.encodeLocation(nameLookup.id));
        HttpURLConnection sessionConn = NetworkUtils.resolveConnection(NetworkUtils.createHttpConnection(sessionUrl));
        int sessionStatus = sessionConn.getResponseCode();
        if (sessionStatus == HttpURLConnection.HTTP_NO_CONTENT || sessionStatus == HttpURLConnection.HTTP_NOT_FOUND) {
            return new ProfileMetadata(false, null, null, null);
        }
        if (sessionStatus != HttpURLConnection.HTTP_OK) {
            throw new IOException("Mojang session profile returned HTTP " + sessionStatus);
        }
        String sessionBody;
        try (InputStream in = sessionConn.getInputStream()) {
            sessionBody = IOUtils.readFullyAsString(in);
        }
        CompleteGameProfile profile = JsonUtils.GSON.fromJson(sessionBody, CompleteGameProfile.class);
        if (profile == null) {
            return new ProfileMetadata(false, null, null, null);
        }

        Optional<Map<TextureType, com.tungsten.fclcore.auth.yggdrasil.Texture>> texturesOpt =
                YggdrasilService.getTextures(profile);
        if (texturesOpt.isEmpty()) {
            return new ProfileMetadata(false, null, null, null);
        }

        Map<TextureType, com.tungsten.fclcore.auth.yggdrasil.Texture> textures = texturesOpt.get();
        com.tungsten.fclcore.auth.yggdrasil.Texture skinTex = textures.get(TextureType.SKIN);
        com.tungsten.fclcore.auth.yggdrasil.Texture capeTex = textures.get(TextureType.CAPE);
        String skinUrl = skinTex != null ? skinTex.getUrl() : null;
        String capeUrl = capeTex != null ? capeTex.getUrl() : null;
        String model = TextureModel.detectModelName(skinTex != null ? skinTex.getMetadata() : null).modelName;
        boolean hasAny = StringUtils.isNotBlank(skinUrl) || StringUtils.isNotBlank(capeUrl);
        return new ProfileMetadata(hasAny, skinUrl, capeUrl, model);
    }

    private static Texture loadRemoteTexture(String url) {
        Texture cached = TEXTURE_CACHE.get(url);
        if (cached != null) {
            return cached;
        }
        byte[] bytes = readDiskTexture(url);
        if (bytes == null) {
            try {
                HttpURLConnection conn = NetworkUtils.resolveConnection(NetworkUtils.createHttpConnection(NetworkUtils.toURL(url)));
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    bytes = IOUtils.readFully(conn.getInputStream());
                    if (bytes != null && bytes.length > 0) {
                        writeDiskTexture(url, bytes);
                    }
                }
            } catch (Exception e) {
                LOG.log(Level.FINE, "Failed to download official Mojang texture: " + url, e);
            }
        }
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            Texture texture = Texture.loadTexture(BitmapFactory.decodeStream(in));
            if (texture != null) {
                TEXTURE_CACHE.putIfAbsent(url, texture);
            }
            return texture;
        } catch (Exception e) {
            LOG.log(Level.FINE, "Failed to decode official Mojang texture: " + url, e);
            return null;
        }
    }

    private static ProfileMetadata readDiskMetadata(String key) {
        Path dir = cacheDir;
        if (dir == null) return null;
        Path file = dir.resolve("profiles").resolve(key + ".json");
        if (!Files.isRegularFile(file)) return null;
        try {
            String json = new String(Files.readAllBytes(file), UTF_8);
            return JsonUtils.GSON.fromJson(json, ProfileMetadata.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static void writeDiskMetadata(String key, ProfileMetadata metadata) {
        Path dir = cacheDir;
        if (dir == null) return;
        Path file = dir.resolve("profiles").resolve(key + ".json");
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, JsonUtils.GSON.toJson(metadata).getBytes(UTF_8));
        } catch (Exception ignored) {
        }
    }

    private static byte[] readDiskTexture(String url) {
        Path dir = cacheDir;
        if (dir == null) return null;
        Path file = dir.resolve("textures").resolve(sha256Hex(url) + ".png");
        if (!Files.isRegularFile(file)) return null;
        try {
            return Files.readAllBytes(file);
        } catch (Exception e) {
            return null;
        }
    }

    private static void writeDiskTexture(String url, byte[] bytes) {
        Path dir = cacheDir;
        if (dir == null) return;
        Path file = dir.resolve("textures").resolve(sha256Hex(url) + ".png");
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, bytes);
        } catch (Exception ignored) {
        }
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Hex.encodeHex(md.digest(input.getBytes(UTF_8)));
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private record CacheEntry(ProfileMetadata metadata, long expiresAtMs) {
    }

    private static final class NameLookupResponse {
        @SerializedName("id")
        String id;
    }

    private static final class ProfileMetadata {
        boolean found;
        String skinUrl;
        String capeUrl;
        String model;

        ProfileMetadata(boolean found, String skinUrl, String capeUrl, String model) {
            this.found = found;
            this.skinUrl = skinUrl;
            this.capeUrl = capeUrl;
            this.model = model;
        }
    }
}
