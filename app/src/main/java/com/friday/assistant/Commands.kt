package com.friday.assistant

sealed interface Command {
    data object Lock : Command
    data object Power : Command
    data object Screen : Command
    data object Briefing : Command
    data class Save(val kind: String, val text: String) : Command
    data class Search(val query: String) : Command
    data class Open(val name: String) : Command
    data class Dial(val number: String) : Command
    data class Remind(val minutes: Long, val text: String) : Command
    data object VoicePassword : Command
    data object Chat : Command
}

/** Only direct user submissions are routed here, never model responses or imported text. */
object Commands {
    fun parse(raw: String): Command {
        val text = raw.trim().replace(Regex("^(hey\\s+)?friday[,!:]?\\s+", RegexOption.IGNORE_CASE), "").trim()
        val lower = text.lowercase().trimEnd('.', '!', '?')
        return when {
            Regex("\\bpass(word|code)?\\s+\\S+", RegexOption.IGNORE_CASE).containsMatchIn(text) && Regex("lock|screen|power|switch off", RegexOption.IGNORE_CASE).containsMatchIn(text) -> Command.VoicePassword
            lower in listOf("lock my phone", "lock phone", "lock screen", "फोन लॉक करो") -> Command.Lock
            lower in listOf("switch off my phone", "turn off my phone", "power menu", "shutdown phone") -> Command.Power
            lower in listOf("read my screen", "read screen", "स्क्रीन पढ़ो") -> Command.Screen
            lower in listOf("morning briefing", "my day", "today's tasks") -> Command.Briefing
            lower.startsWith("remember ") -> Command.Save("memory", text.substring(9).trim())
            lower.startsWith("note ") -> Command.Save("note", text.substring(5).trim())
            lower.startsWith("add task ") -> Command.Save("task", text.substring(9).trim())
            lower.startsWith("search ") -> Command.Search(text.substring(7).trim())
            lower.startsWith("open ") -> Command.Open(text.substring(5).trim())
            Regex("^dial [0-9+ ()-]{3,24}$").matches(lower) -> Command.Dial(text.substring(5))
            else -> {
                val match = Regex("^remind me in (\\d{1,5}) (minute|minutes|hour|hours) to (.+)$", RegexOption.IGNORE_CASE).matchEntire(text)
                if (match != null) {
                    val minutes = match.groupValues[1].toLong() * if (match.groupValues[2].startsWith("hour", true)) 60 else 1
                    if (minutes in 1..43200) Command.Remind(minutes, match.groupValues[3]) else Command.Chat
                } else Command.Chat
            }
        }
    }

    val skills = listOf("AWS", "Azure", "Kubernetes", "Docker", "Terraform", "Jenkins", "GitHub Actions", "Ansible", "Linux", "Python", "Bash", "Helm", "ArgoCD", "Prometheus", "Grafana", "EKS", "AKS", "Trivy", "SonarQube")
    fun jobMatch(resume: String, description: String): Pair<List<String>, List<String>> {
        fun contains(text: String, skill: String) = Regex("(?<![A-Za-z0-9])${Regex.escape(skill)}(?![A-Za-z0-9])", RegexOption.IGNORE_CASE).containsMatchIn(text)
        val required = skills.filter { contains(description, it) }
        return required.filter { contains(resume, it) } to required.filterNot { contains(resume, it) }
    }
}
