package org.findadoge.app.search;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.parse.ParseException;
import com.parse.ParseUser;

import java.util.List;
import java.util.Set;

import edu.gatech.gtri.bktree.BkTreeSearcher;
import edu.gatech.gtri.bktree.Metric;
import edu.gatech.gtri.bktree.MutableBkTree;
import edu.gatech.gtri.stringmetric.DamerauLevenshteinDistance;
import edu.gatech.gtri.stringmetric.StringMetric;

public class SearchSuggestionProvider extends ContentProvider {
    private static final String TAG = "SearchSuggestProvider";

    private static final Metric<ParseUser> damerauLevenshteinDistance = new Metric<ParseUser>() {
        private final StringMetric metric = new DamerauLevenshteinDistance();

        @Override
        public int distance(ParseUser x, ParseUser y) {
            return metric.distance(x.getUsername(), y.getUsername());
        }
    };

    @Override
    public boolean onCreate() {
        // TODO: Implement this to initialize your content provider on startup.
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        String query = selectionArgs[0].toLowerCase();
        MatrixCursor cursor = new MatrixCursor(new String[]{
                BaseColumns._ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA
        });

        if (query.length() < 2)
            return cursor;

        MutableBkTree<ParseUser> bkTree = new MutableBkTree<>(damerauLevenshteinDistance);
        try {
            List<ParseUser> users = ParseUser.getQuery().find();
            for (ParseUser user : users) {
                bkTree.add(user);
            }

            BkTreeSearcher<ParseUser> searcher = new BkTreeSearcher<>(bkTree);

            ParseUser usernameSearch = new ParseUser();
            usernameSearch.setUsername(query);
            Set<BkTreeSearcher.Match<? extends ParseUser>> matches = searcher.search(usernameSearch, 6);

            for (BkTreeSearcher.Match<? extends ParseUser> match : matches) {
                String username = match.getMatch().getUsername();
                cursor.addRow(new Object[]{1, username, username});
            }
        } catch (ParseException e) {
            // Nothing to do here...
        } finally {
            return cursor;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO: Implement this to handle requests to insert a new row.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
