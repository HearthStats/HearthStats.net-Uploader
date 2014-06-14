package net.hearthstats.util;

import java.io.UnsupportedEncodingException;
import java.util.ResourceBundle;

import net.hearthstats.Config;
import net.hearthstats.config.GameLanguage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TranslationCard {
    private TranslationCard() {
    }

    private final static Logger   debugLog = LoggerFactory.getLogger( TranslationCard.class );

    private static ResourceBundle _bundle  = null;

    public static void changeTranslation() {
        GameLanguage lang = Config.gameLanguage();
        switch ( lang ) {

        case FR:
            _bundle = ResourceBundle.getBundle( "net.hearthstats.resources.card.cardFr" );
            break;
        default:
            _bundle = null;
            break;

        }
    }

    public static String t( String key ) {
        String value = _bundle.getString( "card" + key );
        switch ( Config.gameLanguage() ) {
        case FR:
            try {
                return new String( value.getBytes( "ISO-8859-1" ), "UTF-8" );
            } catch ( UnsupportedEncodingException e ) {
                debugLog.debug( "Encoding unsupported  : " + e.getMessage() );
            }
        default:
            return value;
        }

    }

    public static Boolean hasKey( String key ) {
        if ( _bundle == null )
            return false;
        return _bundle.containsKey( "card" + key );
    }
}
