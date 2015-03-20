package net.hearthstats.game

import java.awt.image.BufferedImage
import net.hearthstats.core.Card
import net.hearthstats.game.CardEventType._
import net.hearthstats.core.HeroClass
import net.hearthstats.core.GameMode
import net.hearthstats.core.CardData
import net.hearthstats.core.MatchOutcome
import scala.util.Try

object GameEvents {
  import net.hearthstats.game.Screen._
  import net.hearthstats.game.ScreenGroup._

  implicit def screenToObject(s: Screen): HsScreen = s match {
    case TITLE => TitleScreen
    case MAIN => MainScreen
    case MAIN_TODAYSQUESTS => QuestsScreen
    case ARENA_LOBBY => ArenaLobby
    case ARENA_END => ArenaRunEnd
    case VERSUS_LOBBY => FriendlyLobby
    case PLAY_LOBBY => PlayLobby
    case PRACTICE_LOBBY => PracticeLobby
    case FINDING_OPPONENT => FindingOpponent
    case MATCH_VS => MatchStartScreen
    case MATCH_STARTINGHAND => StartingHandScreen
    case s if s.group == MATCH_PLAYING => OngoingGameScreen
    case s if s.group == MATCH_END => GameResultScreen
    case COLLECTION_DECK => CollectionDeckScreen
    case COLLECTION => CollectionScreen
    case COLLECTION_ZOOM => CollectionScreen
  }
}