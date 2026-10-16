package com.friday.assistant

data class PracticeQuestion(val topic: String, val question: String, val cues: List<String>, val guidance: String)
object Coach {
    val questions = listOf(
        PracticeQuestion("Linux", "Disk usage is 100% but du shows much less data. What do you check?", listOf("df", "du", "lsof", "deleted", "inode"), "Compare df/du and mounts, check inodes and deleted-but-open files with lsof +L1. Identify the owner before rotating or restarting; verify space recovery."),
        PracticeQuestion("Kubernetes", "A deployment becomes unhealthy after a release. How would you investigate and recover?", listOf("events", "logs", "describe", "readiness", "rollback"), "Check rollout status, events, pod describe and current/previous logs. Compare image/config changes, probes and resources. Choose an approved rollback and verify application health."),
        PracticeQuestion("AWS", "Users see intermittent ALB 502 errors. How do you isolate the cause?", listOf("target", "health", "logs", "timeout", "cloudwatch"), "Correlate ALB/target metrics and access logs, target health and application logs. Check connection resets, keep-alive/idle timeouts and recent deployments; validate recovery with requests."),
        PracticeQuestion("CI/CD", "A Jenkins pipeline succeeds but the application runs an old image. What would you check?", listOf("tag", "digest", "registry", "deployment", "verify"), "Trace commit → artifact digest → registry → deployed image. Avoid mutable-tag assumptions; inspect rollout and running container digest, then validate the application version."),
        PracticeQuestion("Terraform", "Terraform reports state locking after an interrupted apply. What would you do?", listOf("lock", "owner", "running", "backup", "plan"), "Confirm the lock owner and whether an apply is still running. Inspect backend health, protect state, and only remove a proven stale lock through an authorised process. Review plan before applying."),
        PracticeQuestion("Security", "A container scanner flags a critical vulnerability before release. How do you respond?", listOf("version", "fix", "base", "scan", "risk"), "Confirm the affected package/version and exploitability, update the dependency/base image, rebuild and rescan. Document residual risk and follow release policy rather than silently bypassing the check.")
    )
    fun review(index: Int, answer: String): String {
        val q = questions[index.mod(questions.size)]
        val covered = q.cues.filter { Regex("(?<![a-z0-9])${Regex.escape(it)}(?![a-z0-9])", RegexOption.IGNORE_CASE).containsMatchIn(answer) }
        val missing = q.cues - covered.toSet()
        return "Practice checklist — not an interview score.\nMentioned: ${covered.joinToString().ifBlank { "No checklist terms found" }}\nRevisit: ${missing.joinToString().ifBlank { "All checklist terms mentioned" }}\n\nSuggested structure: ${q.guidance}\n\nExplain what you would check, why, the recovery decision, and how you would verify it. Keyword presence does not prove correctness."
    }
    fun jobStatus(value: String) = value.takeIf { it in listOf("Saved", "Applied", "Interview", "Offer", "Closed") } ?: "Saved"
}
