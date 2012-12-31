package de.deepamehta.core.storage.spi;

import de.deepamehta.core.storage.ConnectedMehtaEdge;
import de.deepamehta.core.storage.ConnectedMehtaNode;
import de.deepamehta.core.storage.MehtaGraphIndexMode;

import java.util.Set;



/**
 * The common base of {@link MehtaNode}s and {@link MehtaEdge}s.
 *
 * There are methods for:
 *  - Working with attributes (getting, setting, indexing)
 *  - Traversal
 *  - Object Deletion
 */
public interface MehtaObject {

    long getId();

    // --- Get Attributes ---

    /**
     * @throws  Exception    if node has no attribute with that key
     */
    String getString(String key);
    String getString(String key, String defaultValue);

    /**
     * @throws  Exception    if node has no attribute with that key
     */
    int getInteger(String key);
    int getInteger(String key, int defaultValue);

    /**
     * @throws  Exception    if node has no attribute with that key
     */
    boolean getBoolean(String key);
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * @throws  Exception    if node has no attribute with that key
     */
    Object getObject(String key);
    Object getObject(String key, Object defaultValue);

    // ---

    Iterable<String> getAttributeKeys();

    boolean hasAttribute(String key);

    // --- Set Attributes ---

    /**
     * @throws  IllegalArgumentException    if value is null
     */
    String setString(String key, String value);

    Integer setInteger(String key, int value);

    Boolean setBoolean(String key, boolean value);

    /**
     * @throws  IllegalArgumentException    if value is null
     */
    Object setObject(String key, Object value);

    // --- Indexing ---

    /**
     * This method must only be called for index modes OFF and FULLTEXT.
     *
     * @param   oldValue    The value to remove from index. If <code>null</code> no removal is performed.
     */
    void indexAttribute(MehtaGraphIndexMode indexMode, Object value, Object oldValue);

    /**
     * @param   indexKey    Required for index modes KEY and FULLTEXT_KEY. Otherwise ignored.
     * @param   oldValue    The value to remove from index. If <code>null</code> no removal is performed.
     */
    void indexAttribute(MehtaGraphIndexMode indexMode, String indexKey, Object value, Object oldValue);

    // --- Traversal ---

    /**
     * @param   myRoleType      Pass <code>null</code> to switch role type filter off.
     */
    Set<MehtaEdge> getMehtaEdges(String myRoleType);

    // ---

    /**
     * @param   myRoleType      Pass <code>null</code> to switch role type filter off.
     * @param   othersRoleType  Pass <code>null</code> to switch role type filter off.
     */
    ConnectedMehtaNode getConnectedMehtaNode(String myRoleType, String othersRoleType);

    /**
     * @param   myRoleType      Pass <code>null</code> to switch role type filter off.
     * @param   othersRoleType  Pass <code>null</code> to switch role type filter off.
     */
    Set<ConnectedMehtaNode> getConnectedMehtaNodes(String myRoleType, String othersRoleType);

    // ---

    /**
     * @param   myRoleType      Pass <code>null</code> to switch role type filter off.
     * @param   othersRoleType  Pass <code>null</code> to switch role type filter off.
     */
    ConnectedMehtaEdge getConnectedMehtaEdge(String myRoleType, String othersRoleType);

    /**
     * @param   myRoleType      Pass <code>null</code> to switch role type filter off.
     * @param   othersRoleType  Pass <code>null</code> to switch role type filter off.
     */
    Set<ConnectedMehtaEdge> getConnectedMehtaEdges(String myRoleType, String othersRoleType);

    // --- Deletion ---

    /**
     * Deletes this mehta object.
     * Deletion is only possible if the mehta object is not connected to any other mehta objects.
     *
     * @throws  RuntimeException    If this mehta object is connected to other mehta objects.
     */
    void delete();
}
