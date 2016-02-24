package com.bsb.hike.chatthemes;

import com.bsb.hike.db.DBConstants;
import com.bsb.hike.utils.Logger;

/**
 * This class reads and writes data to the tables associated with Chat Themes.
 * The tables associated with chat themes are CHAT_THEME_TABLE, CHAT_THEME_ASSET_TABLE and CHAT_BG_TABLE (constant names in the DBConstants class)
 */
public class ChatThemeDBController implements DBConstants
{

    private final String TAG = "ChatThemeDBController";


    //Methods to create the query for table creation
    /**
     * Method to create a query for creating CHAT_THEME_ASSET_TABLE in the database
     * @return string containing a sql query that creates the CHAT_THEME_ASSET_TABLE
     */
    public String getAssetTableCreateQuery()
    {
        String createAssetTableQuery;

        createAssetTableQuery = CREATE_TABLE + ChatThemes.CHAT_THEME_ASSET_TABLE + " ("
                                + ChatThemes.ASSET_ID_COL               + COLUMN_TYPE_TEXT + " PRIMARY KEY" + COMMA_SEPARATOR
                                + ChatThemes.ASSET_TYPE_COL             + COLUMN_TYPE_INTEGER + COMMA_SEPARATOR
                                + ChatThemes.ASSET_VAL_COL              + COLUMN_TYPE_TEXT + COMMA_SEPARATOR
                                + ChatThemes.ASSET_IS_DOWNLOADED_COL    + COLUMN_TYPE_INTEGER + COMMA_SEPARATOR
                                + ChatThemes.ASSET_TIMESTAMP_COL        + COLUMN_TYPE_INTEGER
                                + ")";

        Logger.d(TAG, "Create Asset Table Query : " + createAssetTableQuery);
        return createAssetTableQuery;
    }

    /**
     * Method to create a query for creating CHAT_THEME_TABLE in the database
     * @return string containing a sql query that create the CHAT_THEME_TABLE
     */
    public String getThemeTableCreateQuery()
    {
        String createThemeTableQuery;

        createThemeTableQuery = CREATE_TABLE + ChatThemes.CHAT_THEME_TABLE + " ("
                                + ChatThemes.BG_ID                                  + COLUMN_TYPE_TEXT + " PRIMARY KEY" + COMMA_SEPARATOR
                                + ChatThemes.THEME_STATUS_COL                       + COLUMN_TYPE_INTEGER + COMMA_SEPARATOR
                                + ChatThemes.BG_ASSET_PORTRAIT_COL                  + COLUMN_TYPE_TEXT + COMMA_SEPARATOR
                                + ChatThemes.BG_ASSET_LANDSCAPE_COL                 + COLUMN_TYPE_TEXT + COMMA_SEPARATOR
                                + ChatThemes.BUBBLE_ASSET_COL                       + COLUMN_TYPE_TEXT + COMMA_SEPARATOR
                                + ChatThemes.HEADER_ASSET_COL                       + COLUMN_TYPE_TEXT + COMMA_SEPARATOR
                                + ChatThemes.SEND_NUDGE_ASSET_COL                   + COLUMN_TYPE_TEXT + COMMA_SEPARATOR
                                + ChatThemes.RECEIVE_NUDGE_ASSET_COL                + COLUMN_TYPE_TEXT + COMMA_SEPARATOR
                                + ChatThemes.INLINE_UPDATE_BG_RESOURCE_COL          + COLUMN_TYPE_TEXT + COMMA_SEPARATOR
                                + ChatThemes.SMS_BG_RESOURCE_COL                    + COLUMN_TYPE_TEXT + COMMA_SEPARATOR
                                + ChatThemes.MULTI_SELECT_BUBBLE_COLOR_ASSET_COL    + COLUMN_TYPE_TEXT + COMMA_SEPARATOR
                                + ChatThemes.OFFLINE_MESSAGE_TEXT_COLOR_COL         + COLUMN_TYPE_TEXT + COMMA_SEPARATOR
                                + ChatThemes.THUMBNAIL_ASSET_COL                    + COLUMN_TYPE_TEXT + COMMA_SEPARATOR
                                + ChatThemes.CT_METADATA_COL                        + COLUMN_TYPE_TEXT
                                + ")";

        Logger.d(TAG, "Create Chat Theme Table Query : " + createThemeTableQuery);
        return createThemeTableQuery;
    }

}
