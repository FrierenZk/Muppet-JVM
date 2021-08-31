package com.github.frierenzk.task

import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileWriter
import java.util.*

@Suppress("SpellCheckingInspection")
class BuildList {
    private class ConfigBuilder(
        var name: String? = null,
        var category: String? = null,
        var profile: String? = null,
        var projectDir: String? = null,
        var source: String? = null,
        var upload: String? = null,
        var local: String? = null,
        var extraParas: HashMap<String, Any> = HashMap(),
    ) {
        fun create(): BuildConfig {
            projectDir.let { if (it is String) extraParas["projectDir"] = it }
            source.let { if (it is String) extraParas["source"] = it }
            upload.let { if (it is String) extraParas["upload"] = it }
            local.let { if (it is String) extraParas["local"] = it }
            return BuildConfig(name!!, category!!, profile!!, extraParas)
        }
    }

    private val list by lazy {
        val categoryBase = "\${base}/\${category}"
        val catv = "\${default}/catv-hgu-sfu-allinone"
        mapOf(
            "maintrunk" to mapOf(
                "hgustandard" to ConfigBuilder(
                    profile = "catv_standard_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv
                ),
                "sfustandard" to ConfigBuilder(
                    profile = "catv_standard_nocolor_sfu_xpon_nowifi_nocable_novoice_nousb",
                    source = catv
                ),
                "wisecable" to ConfigBuilder(
                    profile = "catv_wisecable_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv
                ),
                "fujian" to ConfigBuilder(
                    profile = "catv_fujian_nocolor_hgu_xpon_wifi_cable_voice_usb",
                    source = catv
                ),
                "yueqing" to ConfigBuilder(
                    profile = "catv_yueqing_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv
                ),
                "jingning" to ConfigBuilder(
                    profile = "catv_jingning_white_sfu_epon_nowifi_cable_novoice_nousb",
                    source = catv
                ),
                "huashusfu" to ConfigBuilder(
                    profile = "catv_huashu_nocolor_sfu_xpon_nowifi_cable_novoice_nousb",
                    projectDir = "huashusfu", source = catv
                ),
                "huashusfu-ux3320" to ConfigBuilder(
                    profile = "catv_huashu_ux3320_nocolor_sfu_xpon_nowifi_cable_novoice_nousb",
                    projectDir = "huashusfu", source = catv,
                    upload = "${categoryBase}/huashusfu"
                ),
                "huashu" to ConfigBuilder(
                    profile = "catv_huashu_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv
                ),
                "huashu128" to ConfigBuilder(
                    profile = "catv_huashu_nocolor_sfu_xpon_nowifi_cable_novoice_nousb_128m_nand",
                    source = catv
                ),
                "shaoxing" to ConfigBuilder(
                    profile = "catv_shaoxing_nocolor_sfu_xpon_nowifi_cable_novoice_nousb",
                    source = catv
                ),
                "hunan" to ConfigBuilder(
                    profile = "catv_hunan_nocolor_hgu_xpon_wifi_cable_voice_nousb",
                    source = catv
                ),
                "guangxi" to ConfigBuilder(
                    profile = "catv_guangxi_nocolor_sfu_xpon_nowifi_nocable_novoice_nousb",
                    source = catv
                ),
                "tianjin" to ConfigBuilder(
                    profile = "catv_tianjing_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv
                ),
                "jiangxi-sfu" to ConfigBuilder(
                    profile = "catv_jiangxi_nocolor_sfu_xpon_nowifi_cable_novoice_nousb",
                    source = catv
                ),
                "jiangxi-hgu" to ConfigBuilder(
                    profile = "catv_jiangxi_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv
                ),
                "jiangxi-ux3320" to ConfigBuilder(
                    profile = "catv_jiangxi_ux3320_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv
                ),
                "hubeiguangdian" to ConfigBuilder(
                    profile = "catv_hubeiguangdian_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv
                ),
                "hunanshaoyang" to ConfigBuilder(
                    profile = "catv_hunanshaoyang_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv
                ),
                "neimeng3in1" to ConfigBuilder(
                    profile = "catv_neimeng3in1_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv
                ),
                "shanxijinmei" to ConfigBuilder(
                    profile = "catv_shanxijinmei_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv
                ),
                "shanxi" to ConfigBuilder(
                    profile = "catv_shanxi_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv
                ),
                "ux3320" to ConfigBuilder(
                    profile = "catv_ux3320_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv
                ),
                "columbia" to ConfigBuilder(
                    profile = "catv_columbia_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv
                )
            ),
            "tags" to mapOf(
                "armenia-1.0" to ConfigBuilder(
                    profile = "catv_armenia_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv
                ),
                "guangxi-1.0" to ConfigBuilder(
                    profile = "catv_guangxi_nocolor_hgu_xpon_wifi_cable_voice_usb",
                    source = catv
                ),
                "hunan-3.0" to ConfigBuilder(
                    profile = "catv_hunan_nocolor_hgu_xpon_wifi_cable_voice_nousb",
                    source = catv
                ),
                "hunan-3.0-datong" to ConfigBuilder(
                    profile = "catv_datong_nocolor_hgu_xpon_wifi_cable_voice_nousb",
                    source = catv, upload = "\${base}/\${category}/hunan-3.0"
                ),
                "mexico-1.0" to ConfigBuilder(
                    profile = "catv_mexicanos_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv
                ),
                "mexico-2.0" to ConfigBuilder(
                    profile = "catv_mexicanos_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv
                ),
                "hunan-2.0" to ConfigBuilder(
                    profile = "catv_hunan16_nocolor_sfu_xpon_nowifi_cable_novoice_nousb",
                    source = catv
                ),
                "shandong-1.0" to ConfigBuilder(
                    profile = "catv_shandong16_nocolor_sfu_xpon_nowifi_cable_novoice_nousb",
                    source = catv
                ),
                "heilongjian-2.0" to ConfigBuilder(
                    profile = "catv_heilongjiang_white_hgu_gpon_wifi_nocable_novoice_nousb",
                    projectDir = "heilongjian-2.0"
                ),
                "heilongjian-2.0-nowifi" to ConfigBuilder(
                    profile = "catv_heilongjiang_white_hgu_gpon_nowifi_nocable_novoice_nousb",
                    source = "\${base}/\${category}/heilongjian-2.0",
                    upload = "\${base}/\${category}/heilongjian-2.0",
                    projectDir = "heilongjian-2.0"
                ),
                "shanxi-1.0" to ConfigBuilder(
                    profile = "catv_shanxi_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    projectDir = "shanxi-1.0", source = catv
                ),
                "shanxi-1.0-ux3320" to ConfigBuilder(
                    profile = "catv_shanxi_ux3320_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    projectDir = "shanxi-1.0", source = catv,
                    upload = "\$base/\$category/shanxi-1.0"
                ),
                "hubei-1.0" to ConfigBuilder(
                    profile = "catv_hubeiguangdian_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv
                ),
                "changguang-1.0" to ConfigBuilder(
                    profile = "catv_changguang_nocolor_sfu_xpon_nowifi_cable_novoice_nousb",
                    source = catv
                ),
                "neimeng-2.0" to ConfigBuilder(
                    profile = "catv_neimeng_nocolor_sfu_xpon_nowifi_nocable_novoice_nousb",
                    projectDir = "neimeng-2.0", source = catv
                ),
                "neimeng-2.0-ux3320" to ConfigBuilder(
                    profile = "catv_neimeng_ux3320_nocolor_sfu_xpon_nowifi_nocable_novoice_nousb",
                    projectDir = "neimeng-2.0", source = catv,
                    upload = "\$base/\$category/neimeng-2.0"
                ),
                "columbia-2.0" to ConfigBuilder(
                    profile = "catv_columbia_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv
                ),
                "huashu-1.0" to ConfigBuilder(
                    profile = "catv_huashu_nocolor_sfu_xpon_nowifi_cable_novoice_nousb",
                    source = catv
                ),
            ),
            "branches" to mapOf(
                "FDT_henan" to ConfigBuilder(
                    profile = "catv_henan_nocolor_sfu_xpon_nowifi_nocable_novoice_nousb",
                    source = catv
                ),
                "FDT_henan_xichuan" to ConfigBuilder(
                    profile = "catv_henanxichuan_nocolor_sfu_xpon_nowifi_nocable_novoice_nousb",
                    source = catv,
                    upload = "\$base/\$category/FDT_henan",
                    projectDir = "FDT_henan_xichuan"
                ),
                "FDT_henan_zhengyang" to ConfigBuilder(
                    profile = "catv_henanzhengyang_nocolor_sfu_xpon_nowifi_nocable_novoice_nousb",
                    source = catv,
                    upload = "\$base/\$category/FDT_henan",
                    projectDir = "FDT_henan_xichuan"
                ),
                "FDT_ecuador" to ConfigBuilder(
                    profile = "catv_ecuador_nocolor_hgu_xpon_wifi_cable_voice_usb",
                    source = catv,
                    projectDir = "FDT_ecuador"
                ),
                "tvecuador" to ConfigBuilder(
                    profile = "catv_tvecuador_nocolor_hgu_xpon_wifi_cable_voice_usb",
                    source = catv,
                    projectDir = "FDT_ecuador"
                ),
                "novoice_ecuador" to ConfigBuilder(
                    profile = "catv_ecuador_nocolor_hgu_xpon_wifi_cable_novoice_usb",
                    source = catv,
                    projectDir = "FDT_ecuador"
                ),
                "FDT_SiJie" to ConfigBuilder(
                    profile = "catv_standard_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv,
                ),
                "FDT_dongyan_uGrid" to ConfigBuilder(
                    profile = "catv_dongyanUgrid_black_hgu_gpon_wifi_cable_novoice_usb",
                    source = catv
                ),
                "FDT_dongyan_gpon_neutral" to ConfigBuilder(
                    profile = "catv_dongyanNeutral_black_hgu_gpon_wifi_cable_novoice_usb",
                    source = catv
                ),
                "FDT_ux3320" to ConfigBuilder(
                    profile = "catv_standard_nocolor_hgu_xpon_wifi_nocable_novoice_nousb",
                    source = catv
                ),
                "FDT_fenghuo" to ConfigBuilder(
                    profile = "catv_standard_nocolor_sfu_xpon_nowifi_nocable_novoice_nousb",
                    source = catv
                ),
                "FDT_dualwifi" to ConfigBuilder(
                    profile = "catv_external_nocolor_hgu_xpon_wifi2_nocable_voice_usb",
                    source = catv
                )
            ),
            "7528" to mapOf(
                "7528-sfu" to ConfigBuilder(
                    profile = "catv_general_black_sfu_gpon_nowifi_cable_novoice_nousb",
                    projectDir = "catv_general_black_sfu_gpon_nowifi_cable_novoice_nousb",
                    source = "\${default}/MTK-7528-SFU"
                ),
                "7528-guangxi" to ConfigBuilder(
                    profile = "catv_guangxi_black_sfu_xpon_nowifi_cable_novoice_nousb",
                    projectDir = "guangxi",
                    source = "\${default}/MTK-7528-SFU"
                ),
                "7528-wifi" to ConfigBuilder(
                    profile = "catv_general_black_hgu_gpon_wifi_cable_voice_usb",
                    projectDir = "catv_general_black_hgu_gpon_wifi_cable_voice_usb",
                    source = "\${default}/MTK-7528"
                ),
                "7528-wifi2" to ConfigBuilder(
                    profile = "catv_general_black_hgu_gpon_wifi2_cable_voice_usb",
                    projectDir = "catv_general_black_hgu_gpon_wifi2_cable_voice_usb",
                    source = "\${default}/MTK-7528"
                ),
                "7528-usb2" to ConfigBuilder(
                    profile = "catv_general_black_hgu_gpon_wifi2_nocable_voice2_usb2",
                    projectDir = "catv_general_black_hgu_gpon_wifi2_nocable_voice2_usb2",
                    source = "\${default}/MTK-7528"
                ),
                "7528-fujian" to ConfigBuilder(
                    profile = "catv_fujian_black_hgu_gpon_wifi2_cable_voice_usb",
                    projectDir = "fujian", source = "\${default}/MTK-7528"
                ),
                "7528-osgi" to ConfigBuilder(
                    profile = "catv_general_black_hgu_gpon_wifi2_cable_voice_usb_osgi",
                    projectDir = "osgi", source = "\${default}/MTK-7528"
                ),
            ),
            "7580" to mapOf(
                "MTK-7580" to ConfigBuilder(profile = "CUC_en7580_7592_7615_OSGI_demo"),
                "CUC_en7580_DBUS_7592_7613_demo" to ConfigBuilder(
                    profile = "CUC_en7580_DBUS_7592_7613_demo",
                    source = "\${default}/MTK-CUC-7580"
                ),
                "CUC_en7580_DBUS_7592_7615_demo" to ConfigBuilder(
                    profile = "CUC_en7580_DBUS_7592_7615_demo",
                    source = "\${default}/MTK-CUC-7580"
                ),
            ),
            "MTK-7580" to mapOf(
                "catv_en7580_nocolor_hgu_xpon_wifi2_nocable_voice_usb" to ConfigBuilder(profile = "catv_en7580_nocolor_hgu_xpon_wifi2_nocable_voice_usb"),
            ),
            "MTK-7580-SFU" to mapOf(
                "CT_SFU_EN7580" to ConfigBuilder(profile = "CT_SFU_EN7580"),
                "SIJIE" to ConfigBuilder(profile = "CT_SFU_SIJIE")
            ),
            "wifi6" to mapOf(
                "wifi6_new4" to ConfigBuilder(
                    profile = "CT_EN7561D_LE_7915D_AP_demo",
                    upload = "categoryBase", local = "catv-hgu-sfu-allinone"
                ),
                "wifi6_new4_jianhua" to ConfigBuilder(
                    profile = "CT_EN7561D_LE_7915D_AP_demo_jianhua",
                    upload = categoryBase, source = "catv-hgu-sfu-allinone"
                ),
            )
        )
    }

    fun generate(file: File) {
        if (!file.exists()) file.createNewFile()
        if (file.canWrite()) {
            val writer = FileWriter(file)
            val gson = GsonBuilder().setPrettyPrinting().create()!!
            val data = HashMap<String, SortedMap<String, BuildConfig>>()
            list.forEach { (category, value) ->
                val map = HashMap<String, BuildConfig>()
                value.forEach {
                    it.value.name = it.key
                    it.value.category = category
                    map[it.key] = it.value.create()
                }
                data[category] = map.toSortedMap()
            }
            writer.write(gson.toJson(data.toSortedMap()))
            writer.flush()
            writer.close()
        }
    }
}