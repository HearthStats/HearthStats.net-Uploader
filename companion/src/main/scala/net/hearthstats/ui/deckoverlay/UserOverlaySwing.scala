package net.hearthstats.ui.deckoverlay

import com.softwaremill.macwire.Tagging.{@@ => @@}

import net.hearthstats.config.{Environment, RectangleConfig}
import net.hearthstats.hstatsapi.CardUtils
import net.hearthstats.ui.log.Log
import net.hearthstats.util.Translation

class UserOverlaySwing(
  rectangleConfig: RectangleConfig @@ UserDeckOverlayRectangle,
  cardsTranslation: Translation,
  cardUtils: CardUtils,
  environment: Environment,
  uiLog: Log)

  extends DeckOverlaySwing(
    rectangleConfig,
    cardsTranslation,
    cardUtils,
    environment,
    uiLog)

trait UserDeckOverlayRectangle