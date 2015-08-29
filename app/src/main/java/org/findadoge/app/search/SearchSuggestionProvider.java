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

import org.findadoge.app.model.User;

import java.util.List;
import java.util.Set;

import edu.gatech.gtri.bktree.BkTreeSearcher;
import edu.gatech.gtri.bktree.Metric;
import edu.gatech.gtri.bktree.MutableBkTree;
import edu.gatech.gtri.stringmetric.DamerauLevenshteinDistance;
import edu.gatech.gtri.stringmetric.StringMetric;

public class SearchSuggestionProvider extends ContentProvider {
    private static final String TAG = "SearchSuggestProvider";

    private static final Metric<User> damerauLevenshteinDistance = new Metric<User>() {
        private final StringMetric metric = new DamerauLevenshteinDistance();

        @Override
        public int distance(User x, User y) {
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

        MutableBkTree<User> bkTree = new MutableBkTree<>(damerauLevenshteinDistance);
        try {
            List<User> users = User.getUserQuery().find();
            for (User user : users) {
                bkTree.add(user);
            }

            BkTreeSearcher<User> searcher = new BkTreeSearcher<>(bkTree);

            User usernameSearch = new User();
            usernameSearch.setUsername(query);
            Set<BkTreeSearcher.Match<? extends User>> matches = searcher.search(usernameSearch, 6);

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
