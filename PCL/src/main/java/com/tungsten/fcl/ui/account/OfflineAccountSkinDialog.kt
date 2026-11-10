package com.tungsten.fcl.ui.account

import android.content.Context
import android.graphics.BitmapFactory
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.mio.skin.SkinRenderer
import com.mio.skin.restoreSkinSwitches
import com.mio.util.getScreenHeight
import com.mio.util.getScreenWidth
import com.tungsten.fcl.R
import com.tungsten.fcl.activity.MainActivity
import com.tungsten.fcl.databinding.DialogOfflineAccountSkinBinding
import com.tungsten.fcl.game.TexturesLoader
import com.tungsten.fclauncher.utils.FCLPath
import com.tungsten.fclcore.auth.offline.OfflineAccount
import com.tungsten.fclcore.auth.offline.Skin
import com.tungsten.fclcore.auth.offline.Skin.LoadedSkin
import com.tungsten.fclcore.auth.yggdrasil.TextureModel
import com.tungsten.fclcore.task.Schedulers
import com.tungsten.fclcore.util.Logging
import com.tungsten.fclcore.util.StringUtils
import com.tungsten.fclcore.util.skin.NormalizedSkin
import com.tungsten.fcllibrary.component.dialog.FCLDialog
import com.tungsten.fcllibrary.component.view.FCLTextView
import java.io.File
import java.util.logging.Level

class OfflineAccountSkinDialog(
    context: Context,
    private val account: OfflineAccount,
    private val onUpdated: () -> Unit = {}
) : FCLDialog(context), View.OnClickListener {
    private var binding: DialogOfflineAccountSkinBinding =
        DialogOfflineAccountSkinBinding.inflate(layoutInflater)
    private val renderer: SkinRenderer

    /** Texture model chosen via the classic/slim RadioGroup */
    private var model: TextureModel = TextureModel.STEVE

    init {
        setContentView(binding.root)
        setCancelable(false)

        renderer = SkinRenderer(getContext())
        binding.skinView.setRenderer(renderer, 5f)
        context.restoreSkinSwitches(renderer)

        binding.modelClassic.setOnClickListener(this)
        binding.modelSlim.setOnClickListener(this)
        binding.skinPath.setOnClickListener(this)
        binding.skinPathReset.setOnClickListener(this)
        binding.capePath.setOnClickListener(this)
        binding.capePathReset.setOnClickListener(this)
        binding.positive.setOnClickListener(this)
        binding.negative.setOnClickListener(this)

        binding.officialSkinHint.text =
            context.getString(R.string.account_offline_official_skin_hint, account.username)

        // Restore state from existing skin; non-local slots always fall back to same-name official account
        val existingSkin = account.skin
        if (existingSkin != null && existingSkin.type() == Skin.Type.LOCAL_FILE) {
            model = existingSkin.textureModel()
            binding.skinPathText.string = existingSkin.localSkinPath()
            binding.capePathText.string = existingSkin.localCapePath()
        } else {
            model = TextureModel.detectUUID(account.uuid)
        }
        refreshModelRadio()
    }

    override fun show() {
        val width = getScreenWidth()
        var height = getScreenHeight()
        height = if (height * 2 < width) {
            WindowManager.LayoutParams.MATCH_PARENT
        } else {
            height * 2 / 3
        }
        window?.setLayout(width * 2 / 3, height)
        super.show()
        binding.skinView.onResume()
        refreshSkin()
    }

    override fun dismiss() {
        binding.skinView.onPause()
        super.dismiss()
    }

    private fun refreshSkin() {
        val hasCustomSkin = StringUtils.isNotBlank(binding.skinPathText.string)
        this.skin.load(account.username, account.uuid)
            .whenComplete(
                Schedulers.androidUIThread()
            ) { result: LoadedSkin?, exception: Exception? ->
                if (exception != null) {
                    Logging.LOG.log(Level.WARNING, "Failed to load skin", exception)
                    Toast.makeText(
                        context,
                        context.getString(R.string.message_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val effectiveModel = if (hasCustomSkin) {
                        model
                    } else {
                        result?.model() ?: TextureModel.detectUUID(account.uuid)
                    }
                    val slim = effectiveModel == TextureModel.ALEX
                    renderer.updateTexture(
                        result?.skin()?.image ?: TexturesLoader.getDefaultSkin(effectiveModel).image(),
                        result?.cape()?.image,
                        slim
                    )
                }
            }.start()
    }

    private fun refreshModelRadio() {
        val hasCustomSkin = StringUtils.isNotBlank(binding.skinPathText.string)
        val visibility = if (hasCustomSkin) View.VISIBLE else View.GONE
        binding.modelTitle.visibility = visibility
        binding.modelGroup.visibility = visibility
        binding.modelClassic.setChecked(model == TextureModel.STEVE)
        binding.modelSlim.setChecked(model == TextureModel.ALEX)
    }

    private val skin: Skin
        get() {
            val skinPath = binding.skinPathText.string
            val capePath = binding.capePathText.string
            val hasSkin = StringUtils.isNotBlank(skinPath)
            val hasCape = StringUtils.isNotBlank(capePath)
            return if (hasSkin || hasCape) {
                Skin(
                    Skin.Type.LOCAL_FILE,
                    model,
                    if (hasSkin) skinPath else null,
                    if (hasCape) capePath else null
                )
            } else {
                Skin.createDefault()
            }
        }

    override fun onClick(view: View?) {
        when (view) {
            // Model selection
            binding.modelClassic -> {
                model = TextureModel.STEVE
                refreshModelRadio()
                refreshSkin()
            }

            binding.modelSlim -> {
                model = TextureModel.ALEX
                refreshModelRadio()
                refreshSkin()
            }
            // File selection：先放缓存，点确认时才落位 SKIN_DIR，取消不影响已有皮肤文件
            binding.skinPath -> MainActivity.getInstance().fileLauncher.launchSingleSelection(
                null,
                listOf(".png")
            ) {
                val path = it?.get(0)?.toFile(context, File(FCLPath.CACHE_DIR))?.absolutePath
                    ?: return@launchSingleSelection
                binding.skinPathText.string = path
                runCatching {
                    val bmp = BitmapFactory.decodeFile(path)
                    if (bmp != null) {
                        model = if (NormalizedSkin(bmp).isSlim) TextureModel.ALEX else TextureModel.STEVE
                    }
                }
                refreshModelRadio()
                refreshSkin()
            }

            binding.skinPathReset -> {
                binding.skinPathText.string = null
                refreshModelRadio()
                refreshSkin()
            }

            binding.capePath -> MainActivity.getInstance().fileLauncher.launchSingleSelection(
                null,
                listOf(".png")
            ) {
                binding.capePathText.string =
                    it?.get(0)?.toFile(context, File(FCLPath.CACHE_DIR))?.absolutePath
                        ?: return@launchSingleSelection
                refreshSkin()
            }

            binding.capePathReset -> {
                binding.capePathText.string = null
                refreshSkin()
            }
            // Buttons
            binding.positive -> {
                commitSkinFile(binding.skinPathText, "${account.uuid}.png")
                commitSkinFile(binding.capePathText, "${account.uuid}_cape.png")
                account.setSkin(this.skin)
                MainActivity.getInstance().refreshAvatar(account)
                onUpdated()
                dismiss()
            }

            binding.negative -> dismiss()
        }
    }

    /** 把临时选中的皮肤/披风文件落位到 SKIN_DIR 固定名，或清除已重置的文件 */
    private fun commitSkinFile(text: FCLTextView, fileName: String) {
        val dest = File(FCLPath.SKIN_DIR, fileName)
        val srcPath = text.string
        if (srcPath.isNullOrBlank()) {
            dest.delete()
            return
        }
        val src = File(srcPath)
        if (src.canonicalPath != dest.canonicalPath) {
            runCatching { src.copyTo(dest, overwrite = true) }
                .onSuccess { text.string = dest.absolutePath }
        }
    }
}
