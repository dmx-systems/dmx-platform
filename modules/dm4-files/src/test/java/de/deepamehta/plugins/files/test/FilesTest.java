package de.deepamehta.plugins.files.test;

import de.deepamehta.plugins.files.DirectoryListing;
import de.deepamehta.plugins.files.DirectoryListing.FileItem;
import de.deepamehta.plugins.files.DirectoryListing.ItemKind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import org.codehaus.jettison.json.JSONException;

import java.io.File;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



public class FilesTest {

    // ### FIXME: enable the tests
    // ### They rely on the dm4.filerepo.path system property as defined in global POM

    // @Test
    public void directoryListing() {
        DirectoryListing dir = new DirectoryListing(new File("/"));
        List<FileItem> items = dir.getFileItems();
        FileItem item = items.get(0);
        ItemKind kind = item.getItemKind();
        assertTrue(kind == ItemKind.FILE || kind == ItemKind.DIRECTORY);
    }

    // @Test
    public void directoryListingJSON() {
        try {
            DirectoryListing dir = new DirectoryListing(new File("/"));
            List<FileItem> items = dir.getFileItems();
            FileItem item = items.get(0);
            String kind = item.toJSON().getString("kind");
            assertTrue(kind.equals("file") || kind.equals("directory"));
        } catch (JSONException e) {
            fail();
        }
    }
}
