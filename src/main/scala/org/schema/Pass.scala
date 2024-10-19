package org.schema

/**
  * @param completed Cmp (Completed Passes): The number of successful passes a player makes.
  * @param attended Att (Attempted Passes): The total number of passes a player attempts, regardless of whether they are successful or not.
  * @param completionPercentage Cmp% (Completion Percentage): The percentage of completed passes relative to attempted passes, calculated as (Cmp / Att) * 100.
  * @param progressivePasses PrgP (Progressive Passes): Passes that move the ball significantly forward, typically defined as passes that advance the ball towards the opponent’s goal by at least 10 yards, or into the opponent's penalty area.
  */
final case class Pass(
    completed: Number,
    attended: Number,
    completionPercentage: Number,
    progressivePasses: Number,
)
