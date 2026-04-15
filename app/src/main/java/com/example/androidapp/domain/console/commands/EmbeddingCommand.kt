package com.example.androidapp.domain.console.commands

import com.example.androidapp.domain.console.Command
import com.example.androidapp.domain.console.CommandContext
import com.example.androidapp.domain.console.CommandResult
import com.example.androidapp.domain.console.CompletionSuggestion
import com.example.androidapp.domain.console.OutputLine
import com.example.androidapp.domain.console.OutputStyle
import com.example.androidapp.domain.console.SuggestionType
import com.example.androidapp.domain.model.UserRole
import com.example.androidapp.domain.service.EmbeddingIndex
import com.example.androidapp.domain.service.EmbeddingService

/**
 * Lenh quan ly chi muc embedding cho tim kiem ngu nghia.
 *
 * Ho tro cac lenh con:
 * - `embedding status` -- Hien thi trang thai mo hinh embedding, kich thuoc bo nho dem,
 *   va phien ban mo hinh hien tai.
 * - `embedding reindex` -- Yeu cau tao lai chi muc embedding toan bo. Qua trinh
 *   chay ngam trong nen thong qua WorkManager.
 *
 * Nhan [EmbeddingService] va [EmbeddingIndex] tu tang domain de bao cao
 * trang thai thuc te cua he thong embedding ma khong vi pham kien truc tang.
 */
class EmbeddingCommand(
    private val embeddingService: EmbeddingService,
    private val embeddingIndex: EmbeddingIndex
) : Command {

    override val name: String = "embedding"

    override val aliases: List<String> = listOf("emb")

    override val description: String = "Quan ly chi muc embedding tim kiem ngu nghia"

    override val usage: String = "embedding <status|reindex>"

    override val minimumRole: UserRole = UserRole.USER

    override val category: String = "system"

    override val examples: List<Pair<String, String>> = listOf(
        "embedding status" to "Hien thi trang thai mo hinh embedding va bo nho dem",
        "embedding reindex" to "Yeu cau tao lai chi muc embedding toan bo",
        "emb status" to "Viet tat cua 'embedding status'"
    )

    /**
     * Cac lenh con hop le.
     */
    private val subcommands = listOf("status", "reindex")

    override suspend fun autocomplete(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        if (args.isEmpty() || (args.size == 1 && args[0].isNotEmpty())) {
            val prefix = args.firstOrNull()?.lowercase() ?: ""
            subcommands.filter { it.startsWith(prefix) }.forEach { sub ->
                suggestions.add(
                    CompletionSuggestion(
                        text = sub,
                        description = descriptionForSubcommand(sub),
                        type = SuggestionType.SUBCOMMAND
                    )
                )
            }
        }

        return suggestions
    }

    override suspend fun execute(
        args: List<String>,
        flags: Map<String, String?>,
        context: CommandContext
    ): CommandResult {
        val subcommand = args.firstOrNull()?.lowercase()
            ?: return CommandResult.error(
                "Thieu lenh con. Su dung: $usage\n" +
                        "Cac lenh con: ${subcommands.joinToString(", ")}"
            )

        return when (subcommand) {
            "status" -> executeStatus(context)
            "reindex" -> executeReindex(context)
            else -> CommandResult.error(
                "Lenh con khong hop le: '$subcommand'. Cac lenh con: ${subcommands.joinToString(", ")}"
            )
        }
    }

    /**
     * Hien thi trang thai hien tai cua he thong embedding tim kiem ngu nghia.
     *
     * Bao cao trang thai mo hinh, kich thuoc bo nho dem, va phien ban mo hinh
     * su dung du lieu thuc te tu [EmbeddingService] va [EmbeddingIndex].
     */
    private suspend fun executeStatus(context: CommandContext): CommandResult {
        val lines = mutableListOf<OutputLine>()
        val isOnline = context.services.networkService.isOnline.value
        val modelReady = embeddingService.isReady.value
        val cacheReady = embeddingIndex.isReady.value
        val cacheSize = embeddingIndex.size
        val modelVersion = EmbeddingService.CURRENT_MODEL_VERSION

        lines.add(OutputLine("Trang thai Embedding tim kiem ngu nghia", OutputStyle.HEADER))
        lines.add(OutputLine(""))
        lines.add(
            OutputLine(
                "Mo hinh san sang     : ${if (modelReady) "Co" else "Khong"}",
                if (modelReady) OutputStyle.SUCCESS else OutputStyle.WARNING
            )
        )
        lines.add(
            OutputLine(
                "Bo nho dem san sang  : ${if (cacheReady) "Co" else "Khong"}",
                if (cacheReady) OutputStyle.SUCCESS else OutputStyle.WARNING
            )
        )
        lines.add(
            OutputLine(
                "So luong embedding   : $cacheSize",
                OutputStyle.INFO
            )
        )
        lines.add(
            OutputLine(
                "Phien ban mo hinh    : v$modelVersion",
                OutputStyle.INFO
            )
        )
        lines.add(
            OutputLine(
                "Ket noi mang         : ${if (isOnline) "Truc tuyen" else "Ngoai tuyen"}",
                OutputStyle.INFO
            )
        )
        lines.add(OutputLine(""))

        val overallReady = modelReady && cacheReady
        lines.add(
            OutputLine(
                "Tim kiem ngu nghia   : ${if (overallReady) "Hoat dong" else "Chua san sang"}",
                if (overallReady) OutputStyle.SUCCESS else OutputStyle.WARNING
            )
        )

        if (!modelReady) {
            lines.add(OutputLine(""))
            lines.add(
                OutputLine(
                    "Mo hinh TFLite chua duoc tai. Dam bao file mo hinh MediaPipe Universal Sentence Encoder " +
                            "ton tai trong thu muc assets cua ung dung.",
                    OutputStyle.MUTED
                )
            )
        }

        return CommandResult.success(lines)
    }

    /**
     * Yeu cau tao lai chi muc embedding toan bo.
     *
     * Goi [EmbeddingIndex.requestFullReindex] de kich hoat qua trinh
     * tao lai chi muc chay ngam thong qua WorkManager.
     */
    private fun executeReindex(context: CommandContext): CommandResult {
        val lines = mutableListOf<OutputLine>()

        lines.add(OutputLine("Tao lai chi muc Embedding", OutputStyle.HEADER))
        lines.add(OutputLine(""))

        if (!embeddingService.isReady.value) {
            lines.add(
                OutputLine(
                    "Mo hinh embedding chua san sang. Khong the tao lai chi muc.",
                    OutputStyle.ERROR
                )
            )
            return CommandResult.success(lines)
        }

        val accepted = embeddingIndex.requestFullReindex()
        if (accepted) {
            lines.add(
                OutputLine(
                    "Da bat dau tao lai chi muc embedding.",
                    OutputStyle.SUCCESS
                )
            )
            lines.add(
                OutputLine(
                    "Qua trinh chay ngam thong qua WorkManager.",
                    OutputStyle.INFO
                )
            )
            lines.add(
                OutputLine(
                    "Hien tai co ${embeddingIndex.size} embedding trong bo nho dem.",
                    OutputStyle.INFO
                )
            )
        } else {
            lines.add(
                OutputLine(
                    "Khong the tao lai chi muc. He thong chua duoc cau hinh.",
                    OutputStyle.ERROR
                )
            )
        }

        return CommandResult.success(lines)
    }

    /**
     * Tra ve mo ta cho tung lenh con, hien thi trong goi y tu dong hoan thanh.
     */
    private fun descriptionForSubcommand(sub: String): String = when (sub) {
        "status" -> "Hien thi trang thai mo hinh embedding va bo nho dem"
        "reindex" -> "Tao lai chi muc embedding toan bo"
        else -> ""
    }
}
