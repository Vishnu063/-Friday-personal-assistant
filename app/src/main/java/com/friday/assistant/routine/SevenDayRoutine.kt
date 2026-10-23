package com.friday.assistant.routine

/** Offline 5 AM routine template. No permissions or network required. */
data class RoutineStep(val time: String, val title: String, val category: String)
data class RoutineDay(val number: Int, val focus: String, val action: String)

object SevenDayRoutine {
    const val wakeTime = "05:00"
    const val sleepTime = "21:00"
    val schedule = listOf(
        RoutineStep("05:00", "Wake, hydrate and make bed", "Wellness"),
        RoutineStep("05:15", "Meditation and journaling", "Mind"),
        RoutineStep("05:35", "Walk, stretch or workout", "Fitness"),
        RoutineStep("06:15", "Shower and grooming", "Personal"),
        RoutineStep("06:45", "Balanced breakfast", "Nutrition"),
        RoutineStep("07:15", "DevOps hands-on practice (90 min)", "Career"),
        RoutineStep("08:45", "Break", "Rest"),
        RoutineStep("09:00", "Interview and spoken English practice", "Career"),
        RoutineStep("10:00", "Job applications and follow-ups", "Career"),
        RoutineStep("11:00", "Work or personal projects", "Work"),
        RoutineStep("13:00", "Lunch and walk", "Nutrition"),
        RoutineStep("14:00", "Work or learning commitments", "Work"),
        RoutineStep("17:30", "Outdoor, social or hobby time", "Personal"),
        RoutineStep("19:00", "Dinner", "Nutrition"),
        RoutineStep("19:30", "Prepare tomorrow", "Planning"),
        RoutineStep("20:00", "Screen-free wind down", "Rest"),
        RoutineStep("21:00", "Sleep", "Rest")
    )
    val habits = listOf(
        "Wake at 5 AM", "Move for 30 minutes", "Eat balanced meals",
        "Practice DevOps", "Job application or follow-up",
        "Practice spoken English", "Limit scrolling", "Sleep by 9 PM"
    )
    val challenge = listOf(
        RoutineDay(1, "Reset", "Clean desk and plan week"),
        RoutineDay(2, "Energy", "Exercise and improve meals"),
        RoutineDay(3, "Career", "Troubleshoot DevOps and practice 3 answers"),
        RoutineDay(4, "Discipline", "Finish priorities before entertainment"),
        RoutineDay(5, "Confidence", "Practice communication and grooming"),
        RoutineDay(6, "Action", "Review applications and improve a project"),
        RoutineDay(7, "Review", "Measure progress and plan next week")
    )
}
