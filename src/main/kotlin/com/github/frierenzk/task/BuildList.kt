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
                },
                "huashu" to BuildConfig().apply {
                    profile = "catv_huashu_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "shaoxing" to BuildConfig().apply {
                    profile = "catv_shaoxing_nocolor_sfu_xpon_nowifi_cable_novoice_nousb"
                },
                "hunan" to BuildConfig().apply {
                    profile = "catv_hunan_nocolor_hgu_xpon_wifi_cable_voice_nousb"
                },
                "guangxi-trunk" to BuildConfig().apply {
                    profile = "catv_guangxi_nocolor_sfu_xpon_nowifi_nocable_novoice_nousb"
                    projectDir = "guangxi"
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
                }
            ),
            "tags" to sortedMapOf(
                "armenia" to BuildConfig().apply {
                    profile = "catv_armenia_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                    projectDir = "armenia-1.0"
                },
                "guangxi" to BuildConfig().apply {
                    profile = "catv_guangxi_nocolor_hgu_xpon_wifi_cable_voice_usb"
                    projectDir = "guangxi-1.0"
                },
                "hunan-3.0" to BuildConfig().apply {
                    profile = "catv_hunan_nocolor_hgu_xpon_wifi_cable_voice_nousb"
                },
                "mexico-1.0" to BuildConfig().apply {
                    profile = "catv_mexicanos_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "mexico-2.0" to BuildConfig().apply {
                    profile = "catv_mexicanos_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "shandong" to BuildConfig().apply {
                    profile = "catv_shandong16_nocolor_sfu_xpon_nowifi_cable_novoice_nousb"
                    projectDir = "hunan-2.0"
                    uploadPath = "\$base/\$category/shandong-1.0"
                },
                "shandong-1.0" to BuildConfig().apply {
                    profile = "catv_shandong16_nocolor_sfu_xpon_nowifi_cable_novoice_nousb"
                },
                "heilongjian-2.0" to BuildConfig().apply {
                    profile = "catv_heilongjiang_white_hgu_gpon_wifi_nocable_novoice_nousb"
                    sourcePath = "\$default"
                },
                "heilongjian-2.0-nowifi" to BuildConfig().apply {
                    profile = "catv_heilongjiang_white_hgu_gpon_nowifi_nocable_novoice_nousb"
                    sourcePath = "\$base/\$category/heilongjian-2.0"
                    uploadPath = "\$base/\$category/heilongjian-2.0"
                },
                "shanxi-1.0" to BuildConfig().apply {
                    profile = "catv_shanxi_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "hubei-1.0" to BuildConfig().apply {
                    profile = "catv_hubeiguangdian_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "changguang-1.0" to BuildConfig().apply {
                    profile = "catv_changguang_nocolor_sfu_xpon_nowifi_cable_novoice_nousb"
                }
            ),
            "branches" to sortedMapOf(
                "FDT_henan" to BuildConfig().apply {
                    profile = "catv_henan_nocolor_sfu_xpon_nowifi_nocable_novoice_nousb"
                },
                "ecuador" to BuildConfig().apply {
                    profile = "catv_ecuador_nocolor_hgu_xpon_wifi_cable_voice_usb"
                    projectDir = "FDT_ecuador"
                },
                "tvecuador" to BuildConfig().apply {
                    profile = "catv_tvecuador_nocolor_hgu_xpon_wifi_cable_voice_usb"
                    projectDir = "FDT_ecuador"
                },
                "FDT_SiJie" to BuildConfig().apply {
                    profile = "catv_standard_nocolor_hgu_xpon_wifi_nocable_novoice_nousb"
                },
                "FDT_dongyan_uGrid" to BuildConfig().apply {
                    profile = "catv_dongyanUgrid_black_hgu_gpon_wifi_cable_novoice_usb"
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
                    sourcePath = "\$default/MTK-7580"
                },
                "7528-wifi2" to BuildConfig().apply {
                    profile = "catv_general_black_hgu_gpon_wifi2_cable_voice_usb"
                    projectDir = "catv_general_black_hgu_gpon_wifi2_cable_voice_usb"
                    sourcePath = "\$default/MTK-7580"
                },
                "7528-fujian" to BuildConfig().apply {
                    profile = "catv_fujian_black_hgu_gpon_wifi2_cable_voice_usb"
                    projectDir = "fujian"
                    sourcePath = "\$default/MTK-7580"
                },
                "7528-osgi" to BuildConfig().apply {
                    profile = "catv_general_black_hgu_gpon_wifi2_cable_voice_usb_osgi"
                    projectDir = "osgi"
                    sourcePath = "\$default/MTK-7580"
                }
            ),
            "7580" to sortedMapOf(
                "7580" to BuildConfig().apply {
                    profile = "CUC_en7580_7592_7615_OSGI_demo"
                    projectDir = "MTK-7580"
                    sourcePath = "\$default"
                },
                "7580-sfu" to BuildConfig().apply {
                    profile = "CUC_en7580_SFU_demo"
                    projectDir = "CUC_en7580_SFU_demo"
                    sourcePath = "\$default/MTK-7580"
                },
                "7580-hgu-new" to BuildConfig().apply {
                    profile = "CUC_en7580_7592_7615"
                    projectDir = "CUC_en7580_7592_7615"
                },
                "7580-sfu-new" to BuildConfig().apply {
                    profile = "CUC_en7580_7592_7615_SFU"
                    projectDir = "CUC_en7580_7592_7615_SFU"
                },
                "7580-sfu-catv" to BuildConfig().apply {
                    profile = "catv_en7580_sfu"
                    projectDir = "catv_en7580_sfu"
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