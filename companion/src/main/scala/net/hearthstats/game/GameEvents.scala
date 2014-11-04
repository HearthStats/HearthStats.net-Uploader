package net.hearthstats.game

import java.awt.image.BufferedImage

import net.hearthstats.core.Card
import net.hearthstats.game.CardEventType._

sealed trait GameEvent
sealed trait HeroEvent extends GameEvent

case object TurnPassedEvent extends GameEvent
case class HeroPowerEvent(cardId: String, player: Int) extends GameEvent
case class HeroPowerDeclared(cardId: String, player: Int) extends GameEvent

case class CardEvent(card: String, eventType: CardEventType) extends GameEvent

object CardEvents {
  def CardPlayed(card: String) = CardEvent(card, PLAYED)
  def CardReturned(card: String) = CardEvent(card, RETURNED)
  def CardDrawn(card: String) = CardEvent(card, DRAWN)
  def CardReplaced(card: String) = CardEvent(card, REPLACED)
  def CardDiscarded(card: String) = CardEvent(card, DISCARDED)
  def CardPutInPlay(card: String) = CardEvent(card, PUT_IN_PLAY)
  def CardDestroyed(card: String) = CardEvent(card, DESTROYED)
}

case class HeroDestroyedEvent(opponent: Boolean) extends HeroEvent
case class HeroChosen(hero: String, id: Int, opponent: Boolean, player: Int) extends HeroEvent

case class ScreenEvent(screen: HsScreen, image: BufferedImage) extends GameEvent {
  override def toString = s"ScreenEvent($screen)"
}

sealed trait HsScreen
case object TitleScreen extends HsScreen
case object MainScreen extends HsScreen
case object QuestsScreen extends HsScreen
case object ArenaLobby extends HsScreen
case object ArenaRunEnd extends HsScreen
case object FriendlyLobby extends HsScreen
case object PlayLobby extends HsScreen
case object PracticeLobby extends HsScreen
case object FindingOpponent extends HsScreen
case object MatchStartScreen extends HsScreen
case object StartingHandScreen extends HsScreen
case object OngoingGameScreen extends HsScreen
case object GameResultScreen extends HsScreen
case object CollectionScreen extends HsScreen
case object CollectionDeckScreen extends HsScreen

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