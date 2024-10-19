package org.schema

final case class PlayerStat (
    league: String, 
    season: String, 
    game: String, 
    // team: String, 
    player: String, 
    // jersey_number: String, 
    // nation: String, 
    // pos: String, 
    // age: String, 
    // min: String,
    performance: Performance,
    // expectation: Expectation,
    sca: ShotCreatingAction,
)
