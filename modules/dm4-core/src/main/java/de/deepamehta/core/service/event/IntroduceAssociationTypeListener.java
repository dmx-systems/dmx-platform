package de.deepamehta.core.service.event;

import de.deepamehta.core.AssociationType;
import de.deepamehta.core.service.EventListener;



/**
 * ### FIXDOC
 * Allows a plugin to modify type definitions -- exisisting ones <i>and</i> future ones.
 * Plugins get a opportunity to visit (and modify) each type definition extacly once.
 * <p>
 * This hook is triggered in 2 situations:
 * <ul>
 *  <li>for each type that <i>exists</i> already while a plugin clean install.
 *  <li>for types created (interactively by the user, or programmatically by a migration) <i>after</i>
 *      the plugin has been installed.
 * </ul>
 * This hook is typically used by plugins which provide cross-cutting concerns by affecting <i>all</i>
 * type definitions of a DeepaMehta installation. Typically such a plugin adds new data fields to types
 * or relates types with specific topics.
 * <p>
 * Examples of plugins which use this hook:
 * <ul>
 *  <li>The "DeepaMehta 4 Workspaces" plugin adds a "Workspaces" field to all types.
 *  <li>The "DeepaMehta 4 Time" plugin adds timestamp fields to all types.
 *  <li>The "DeepaMehta 4 Access Control" plugin adds a "Creator" field to all types and relates them to a user.
 * </ul>
 *
 * @param   assocType   the type to be modified. The passed object is actually an instance of a {@link AssociationType}
 *                      subclass that is backed by the database. That is, modifications by e.g.
 *                      {@link AssociationType#addDataField} are persistent.
 *                      <p>
 *                      Note: at the time the hook is triggered the type exists already in the database, in
 *                      particular the underlying type topic has an ID already. That is, the type is ready for
 *                      e.g. being related to other topics.
 */
public interface IntroduceAssociationTypeListener extends EventListener {

    void introduceAssociationType(AssociationType assocType);
}
