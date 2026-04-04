package com.example.dataproviderapp.utils

import android.content.Context
import androidx.appcompat.app.AlertDialog

object Utils {
    fun showErrorDialog(message: String, context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Ошибка")
            .setMessage(message)
            .setPositiveButton("ОК", null)
            .show()
    }

    fun showNetworkErrorDialog(context: Context) {
        showErrorDialog("Нет подключения к интернету", context)
    }

    fun showUnknownErrorDialog(context: Context) {
        showErrorDialog("Неизвестная ошибка", context)
    }

    fun showErrorDialogWithAction(message: String, context: Context, action: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Ошибка")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("ОК") { _, _ ->
                action()
            }
            .show()
    }
}