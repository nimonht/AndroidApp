package com.example.androidapp.domain.console.commands

import com.example.androidapp.domain.console.Command
import com.example.androidapp.domain.console.CommandContext
import com.example.androidapp.domain.console.CommandResult
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.OutputLine
import com.example.androidapp.domain.console.OutputStyle

/**
 * Lenh xoa man hinh console.
 *
 * Tra ve mot ket qua dac biet chua gia tri "__CLEAR__" ma ViewModel
 * se nhan dien de xoa toan bo lich su hien thi. Ban than lenh khong
 * thuc hien xoa — no chi gui tin hieu len tang UI.
 *
 * Su dung: `clear`
 */
class ClearCommand : Command {

    override val name: String = "clear"

    override val aliases: List<String> = listOf("cls", "clr")

    override val description: String = "Xoa man hinh console"

    override val usage: String = "clear"

    override val category: String = "util"

    override val examples: List<Pair<String, String>> = listOf(
        "clear" to "Xoa toan bo noi dung hien thi tren console"
    )

    /**
     * Khong co tham so hay co nao de goi y — tra ve danh sach rong.
     */
    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> = emptyList()

    /**
     * Tra ve ket qua chua gia tri dac biet `__CLEAR__`.
     *
     * ViewModel kiem tra dong dau ra dau tien: neu text bang `__CLEAR__`
     * thi xoa toan bo buffer hien thi thay vi hien thi dong nay.
     *
     * @param args Khong su dung.
     * @param flags Khong su dung.
     * @param context Khong su dung.
     * @return [CommandResult] voi mot dong chua ma `__CLEAR__`.
     */
    override suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        return CommandResult(
            output = listOf(OutputLine(CLEAR_SIGNAL, OutputStyle.NORMAL)),
            isSuccess = true
        )
    }

    companion object {
        /**
         * Gia tri dac biet ma ViewModel dung de nhan biet lenh xoa man hinh.
         */
        const val CLEAR_SIGNAL = "__CLEAR__"
    }
}
