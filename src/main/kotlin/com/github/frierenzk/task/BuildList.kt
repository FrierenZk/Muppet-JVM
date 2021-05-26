package com.github.frierenzk.task

import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileWriter

@Suppress("SpellCheckingInspection")
class BuildList {
    private val list by lazy {
        sortedMapOf(
            "maintrunk" to sortedMapOf(
                "hgustandard" to BuildConfig().apply {
                    profile = "catv_standard_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "sfustandard" to BuildConfig().apply {
                    profile = "catv_standard_nocolor_sfu_xpon_nowifi_nocable_novoice_nousb"
                },
                "wisecable" to BuildConfig().apply {
                    profile = "catv_wisecable_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "fujian" to BuildConfig().apply {
                    profile = "catv_fujian_nocolor_hgu_xpon_wifi_cable_voice_usb"
                },
                "yueqing" to BuildConfig().apply {
                    profile = "catv_yueqing_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "jingning" to BuildConfig().apply {
                    profile = "catv_jingning_white_sfu_epon_nowifi_cable_novoice_nousb"
                },
                "huashusfu" to BuildConfig().apply {
                    profile = "catv_huashu_nocolor_sfu_xpon_nowifi_cable_novoice_nousb"
                    projectDir = "huashusfu"
                },
                "huashusfu-ux3320" to BuildConfig().apply {
                    profile = "catv_huashu_ux3320_nocolor_sfu_xpon_nowifi_cable_novoice_nousb"
                    projectDir = "huashusfu"
                    uploadPath = "\$base/\$category/huashusfu"
                },
                "huashu" to BuildConfig().apply {
                    profile = "catv_huashu_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "huashu128" to BuildConfig().apply {
                    profile = "catv_huashu_nocolor_sfu_xpon_nowifi_cable_novoice_nousb_128m_nand"
                },
                "shaoxing" to BuildConfig().apply {
                    profile = "catv_shaoxing_nocolor_sfu_xpon_nowifi_cable_novoice_nousb"
                },
                "hunan" to BuildConfig().apply {
                    profile = "catv_hunan_nocolor_hgu_xpon_wifi_cable_voice_nousb"
                },
                "guangxi" to BuildConfig().apply {
                    profile = "catv_guangxi_nocolor_sfu_xpon_nowifi_nocable_novoice_nousb"
                },
                "tianjin" to BuildConfig().apply {
                    profile = "catv_tianjing_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "jiangxi-sfu" to BuildConfig().apply {
                    profile = "catv_jiangxi_nocolor_sfu_xpon_nowifi_nocable_novoice_nousb"
                },
                "jiangxi-hgu" to BuildConfig().apply {
                    profile = "catv_jiangxi_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "hubeiguangdian" to BuildConfig().apply {
                    profile = "catv_hubeiguangdian_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "hunanshaoyang" to BuildConfig().apply {
                    profile = "catv_hunanshaoyang_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "neimeng3in1" to BuildConfig().apply {
                    profile = "catv_neimeng3in1_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "shanxijinmei" to BuildConfig().apply {
                    profile = "catv_shanxijinmei_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "shanxi" to BuildConfig().apply {
                    profile = "catv_shanxi_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "ux3320" to BuildConfig().apply {
                    profile = "catv_ux3320_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "columbia" to BuildConfig().apply {
                    profile = "catv_columbia_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                }
            ),
            "tags" to sortedMapOf(
                "armenia-1.0" to BuildConfig().apply {
                    profile = "catv_armenia_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "guangxi-1.0" to BuildConfig().apply {
                    profile = "catv_guangxi_nocolor_hgu_xpon_wifi_cable_voice_usb"
                },
                "hunan-3.0" to BuildConfig().apply {
                    profile = "catv_hunan_nocolor_hgu_xpon_wifi_cable_voice_nousb"
                },
                "hunan-3.0-datong" to BuildConfig().apply {
                    profile = "catv_datong_nocolor_hgu_xpon_wifi_cable_voice_nousb"
                    uploadPath = "\$base/\$category/hunan-3.0"
                },
                "mexico-1.0" to BuildConfig().apply {
                    profile = "catv_mexicanos_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "mexico-2.0" to BuildConfig().apply {
                    profile = "catv_mexicanos_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "hunan-2.0" to BuildConfig().apply {
                    profile = "catv_hunan16_nocolor_sfu_xpon_nowifi_cable_novoice_nousb"
                },
                "shandong-1.0" to BuildConfig().apply {
                    profile = "catv_shandong16_nocolor_sfu_xpon_nowifi_cable_novoice_nousb"
                },
                "heilongjian-2.0" to BuildConfig().apply {
                    profile = "catv_heilongjiang_white_hgu_gpon_wifi_nocable_novoice_nousb"
                    sourcePath = "\$default"
                    projectDir = "heilongjian-2.0"
                },
                "heilongjian-2.0-nowifi" to BuildConfig().apply {
                    profile = "catv_heilongjiang_white_hgu_gpon_nowifi_nocable_novoice_nousb"
                    uploadPath = "\$base/\$category/heilongjian-2.0"
                    projectDir = "heilongjian-2.0"
                },
                "shanxi-1.0" to BuildConfig().apply {
                    profile = "catv_shanxi_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                    projectDir = "shanxi-1.0"
                },
                "shanxi-1.0-ux3320" to BuildConfig().apply {
                    profile = "catv_shanxi_ux3320_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                    projectDir = "shanxi-1.0"
                    uploadPath = "\$base/\$category/shanxi-1.0"
                },
                "hubei-1.0" to BuildConfig().apply {
                    profile = "catv_hubeiguangdian_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "changguang-1.0" to BuildConfig().apply {
                    profile = "catv_changguang_nocolor_sfu_xpon_nowifi_cable_novoice_nousb"
                },
                "neimeng-2.0" to BuildConfig().apply {
                    profile = "catv_neimeng_nocolor_sfu_xpon_nowifi_nocable_novoice_nousb"
                    projectDir = "neimeng-2.0"
                },
                "neimeng-2.0-ux3320" to BuildConfig().apply {
                    profile = "catv_neimeng_ux3320_nocolor_sfu_xpon_nowifi_nocable_novoice_nousb"
                    projectDir = "neimeng-2.0"
                    uploadPath = "\$base/\$category/neimeng-2.0"
                },
                "columbia-2.0" to BuildConfig().apply {
                    profile = "catv_columbia_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "huashu-1.0" to BuildConfig().apply {
                    profile = "catv_huashu_nocolor_sfu_xpon_nowifi_cable_novoice_nousb"
                }
            ),
            "branches" to sortedMapOf(
                "FDT_henan" to BuildConfig().apply {
                    profile = "catv_henan_nocolor_sfu_xpon_nowifi_nocable_novoice_nousb"
                },
                "FDT_henan_xichuan" to BuildConfig().apply {
                    profile = "catv_henanxichuan_nocolor_sfu_xpon_nowifi_nocable_novoice_nousb"
                    uploadPath = "\$base/\$category/FDT_henan"
                    projectDir = "FDT_henan_xichuan"
                },
                "FDT_henan_zhengyang" to BuildConfig().apply {
                    profile = "catv_henanzhengyang_nocolor_sfu_xpon_nowifi_nocable_novoice_nousb"
                    uploadPath = "\$base/\$category/FDT_henan"
                    projectDir = "FDT_henan_xichuan"
                },
                "FDT_ecuador" to BuildConfig().apply {
                    profile = "catv_ecuador_nocolor_hgu_xpon_wifi_cable_voice_usb"
                    projectDir = "FDT_ecuador"
                },
                "tvecuador" to BuildConfig().apply {
                    profile = "catv_tvecuador_nocolor_hgu_xpon_wifi_cable_voice_usb"
                    projectDir = "FDT_ecuador"
                },
                "novoice_ecuador" to BuildConfig().apply {
                    profile = "catv_ecuador_nocolor_hgu_xpon_wifi_cable_novoice_usb"
                    projectDir = "FDT_ecuador"
                },
                "FDT_SiJie" to BuildConfig().apply {
                    profile = "catv_standard_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "FDT_dongyan_uGrid" to BuildConfig().apply {
                    profile = "catv_dongyanUgrid_black_hgu_gpon_wifi_cable_novoice_usb"
                },
                "FDT_dongyan_gpon_neutral" to BuildConfig().apply {
                    profile = "catv_dongyanNeutral_black_hgu_gpon_wifi_cable_novoice_usb"
                },
                "FDT_ux3320" to BuildConfig().apply {
                    profile = "catv_standard_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "FDT_fenghuo" to BuildConfig().apply {
                    profile = "catv_standard_nocolor_sfu_xpon_nowifi_nocable_novoice_nousb"
                }
            ),
            "7528" to sortedMapOf(
                "7528-sfu" to BuildConfig().apply {
                    profile = "catv_general_black_sfu_gpon_nowifi_cable_novoice_nousb"
                    projectDir = "catv_general_black_sfu_gpon_nowifi_cable_novoice_nousb"
                    sourcePath = "\$default/MTK-7528-SFU"
                },
                "7528-guangxi" to BuildConfig().apply {
                    profile = "catv_guangxi_black_sfu_xpon_nowifi_cable_novoice_nousb"
                    projectDir = "guangxi"
                    sourcePath = "\$default/MTK-7528-SFU"
                },
                "7528-wifi" to BuildConfig().apply {
                    profile = "catv_general_black_hgu_gpon_wifi_cable_voice_usb"
                    projectDir = "catv_general_black_hgu_gpon_wifi_cable_voice_usb"
                    sourcePath = "\$default/MTK-7528"
                },
                "7528-wifi2" to BuildConfig().apply {
                    profile = "catv_general_black_hgu_gpon_wifi2_cable_voice_usb"
                    projectDir = "catv_general_black_hgu_gpon_wifi2_cable_voice_usb"
                    sourcePath = "\$default/MTK-7528"
                },
                "7528-usb2" to BuildConfig().apply {
                    profile = "catv_general_black_hgu_gpon_wifi2_nocable_voice2_usb2"
                    projectDir = "catv_general_black_hgu_gpon_wifi2_nocable_voice2_usb2"
                    sourcePath = "\$default/MTK-7528"
                },
                "7528-fujian" to BuildConfig().apply {
                    profile = "catv_fujian_black_hgu_gpon_wifi2_cable_voice_usb"
                    projectDir = "fujian"
                    sourcePath = "\$default/MTK-7528"
                },
                "7528-osgi" to BuildConfig().apply {
                    profile = "catv_general_black_hgu_gpon_wifi2_cable_voice_usb_osgi"
                    projectDir = "osgi"
                    sourcePath = "\$default/MTK-7528"
                }
            ),
            "7580" to sortedMapOf(
                "MTK-7580" to BuildConfig().apply {
                    profile = "CUC_en7580_7592_7615_OSGI_demo"
                    sourcePath = "\$default"
                },
                "MTK-7580-SFU" to BuildConfig().apply {
                    profile = "CT_SFU_EN7580"
                    sourcePath = "\$default"
                },
                "CUC_en7580_DBUS_7592_7613_demo" to BuildConfig().apply {
                    profile = "CUC_en7580_DBUS_7592_7613_demo"
                    sourcePath = "\$default/MTK-CUC-7580"
                },
                "CUC_en7580_DBUS_7592_7615_demo" to BuildConfig().apply {
                    profile = "CUC_en7580_DBUS_7592_7615_demo"
                    sourcePath = "\$default/MTK-CUC-7580"
                }
            ),
            "wifi6" to sortedMapOf(
                "wifi6" to BuildConfig().apply {
                    profile = "CT_EN7561D_LE_7915D_AP_demo"
                    uploadPath = "\$base/\$category"
                },
                "wifi6_new" to BuildConfig().apply {
                    profile = "CT_EN7561D_LE_7915D_AP_demo"
                    uploadPath = "\$base/\$category"
                },
                "wifi6_new1" to BuildConfig().apply {
                    profile = "CT_EN7561D_LE_7915D_AP_demo"
                    uploadPath = "\$base/\$category"
                },
                "wifi6_new2" to BuildConfig().apply {
                    profile = "CT_EN7561D_LE_7915D_AP_demo"
                    uploadPath = "\$base/\$category"
                },
                "wifi6_new2_jianhua" to BuildConfig().apply {
                    profile = "CT_EN7561D_LE_7915D_AP_demo_jianhua"
                    uploadPath = "\$base/\$category"
                },
                "wifi6_new4" to BuildConfig().apply {
                    profile = "CT_EN7561D_LE_7915D_AP_demo"
                    uploadPath = "\$base/\$category"
                },
                "wifi6_new4_jianhua" to BuildConfig().apply {
                    profile = "CT_EN7561D_LE_7915D_AP_demo_jianhua"
                    uploadPath = "\$base/\$category"
                }
            )
        )
    }

    fun generate(file: File) {
        if (!file.exists()) file.createNewFile()
        if (file.canWrite()) {
            val writer = FileWriter(file)
            val gson = GsonBuilder().setPrettyPrinting().create()!!
            list.forEach { (category, value) ->
                value?.forEach {
                    it.value.category = category
                    it.value.name = it.key
                }
            }
            writer.write(gson.toJson(list))
            writer.flush()
            writer.close()
        }
    }
}