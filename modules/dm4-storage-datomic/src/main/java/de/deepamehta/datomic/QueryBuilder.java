package de.deepamehta.datomic;

import static de.deepamehta.datomic.DatomicStorage.ident;

import datomic.Database;
import datomic.QueryRequest;
import static datomic.Util.list;
import static datomic.Util.map;
import static datomic.Util.read;

import java.util.ArrayList;
import java.util.List;



class QueryBuilder {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final Object RULES = read("[" +
        // entity type filter: entity ?e is of type ?t
        "[(entity-type ?e ?t)" +
        " [?e :dm4/entity-type ?t]]" +
        // object type filter: object ?o (topic or assoc) is of type ?t
        "[(object-type ?o ?t)" +
        " [?o :dm4.object/type ?t]]" +
        // object value filter: object ?o's (topic or assoc) value for attribute ?a is ?v
        "[(object-value ?o ?a ?v)" +
        " [?o ?a ?v]]" +
        // association filter: ?e1 is associated to ?e2 via ?a; the roles are ?r1 and ?r2
        "[(association ?e1 ?e2 ?a ?r1 ?r2)" +
        " [?r1 :dm4.role/player ?e1]" +
        " [?a :dm4.assoc/role ?r1]" +
        " [?a :dm4.assoc/role ?r2]" +
        " [(!= ?r1 ?r2)]" +
        " [?r2 :dm4.role/player ?e2]]" +
        // role type filter: role ?r is of type ?t
        "[(role-type ?r ?t)" +
        " [?r :dm4.role/type ?t]]" +
    "]");

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Database db;

    // ---------------------------------------------------------------------------------------------------- Constructors

    QueryBuilder(Database db) {
        this.db = db;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    QueryRequest keyValue(EntityType entityType, String key, Object value) {
        return queryRequest(
            "[:find [?e ...] :in $ % ?et ?a ?v :where (object-value ?e ?a ?v) (entity-type ?e ?et)]",
            db, RULES, entityType.ident, ident(key), value
        );
    }

    QueryRequest byType(EntityType entityType, String typeUri) {
        return queryRequest(
            "[:find [?e ...] :in $ % ?et ?ot :where (object-type ?e ?ot) (entity-type ?e ?et)]",
            db, RULES, entityType.ident, ident(typeUri)
        );
    }

    // ### FIXME: process objectId2
    // ### FIXME: entity type check on objectId1
    /**
     * Builds a query to get associations a given <i>start object</i> (topic or association) is involved in.
     * Several association filters can be supplied.
     *
     * @param   assocTypeUri    Association type filter.
     *                          Optional. Pass <code>null</code> for no filtering.
     *
     * @param   roleTypeUri1    Role type filter for the start object's end.
     *                          Optional. Pass <code>null</code> for no filtering.
     * @param   entityType1     Entity type (<code>TOPIC</code> or <code>ASSOC</code>) filter for the start object's
     *                          end. Optional. Pass <code>null</code> for no filtering.
     * @param   objectId1       The start object (topic or association).
     *                          <b>Mandatory</b>. Don't pass <code>-1</code>.
     * @param   objectTypeUri1  Object type (topic type or association type) filter for the start object's end.
     *                          Optional. Pass <code>null</code> for no filtering.
     */
    QueryRequest associations(String assocTypeUri,
            String roleTypeUri1, EntityType entityType1, long objectId1, String objectTypeUri1,
            String roleTypeUri2, EntityType entityType2, long objectId2, String objectTypeUri2) {
        //
        if (objectId1 == -1) {
            throw new IllegalArgumentException("-1 is passed as start object");
        }
        //
        List find   = new ArrayList(); find.add(read("?e2")); find.add(read("?a"));
        List in     = new ArrayList(); in.add(read("$")); in.add(read("%")); in.add(read("?e1"));
        List where  = new ArrayList(); where.add(read("(association ?e1 ?e2 ?a ?r1 ?r2)"));
        List inputs = new ArrayList(); inputs.add(db); inputs.add(RULES); inputs.add(objectId1);
        //
        // entity type 1
        if (entityType1 != null) {
            where.add(read("(entity-type ?e1 ?et1)"));
            in.add(read("?et1"));
            inputs.add(entityType1.ident);
        }
        // object type 1
        if (objectTypeUri1 != null) {
            where.add(read("(object-type ?e1 ?ot1)"));
            in.add(read("?ot1"));
            inputs.add(ident(objectTypeUri1));
        }
        // assoc type
        if (assocTypeUri != null) {
            where.add(read("(object-type ?a ?at)"));
            in.add(read("?at"));
            inputs.add(ident(assocTypeUri));
        }
        // role types
        if (roleTypeUri1 != null) {
            where.add(read("(role-type ?r1 ?rt1)"));
            in.add(read("?rt1"));
            inputs.add(ident(roleTypeUri1));
        }
        if (roleTypeUri2 != null) {
            where.add(read("(role-type ?r2 ?rt2)"));
            in.add(read("?rt2"));
            inputs.add(ident(roleTypeUri2));
        }
        // entity type 2
        if (entityType2 != null) {
            where.add(read("(entity-type ?e2 ?et2)"));
            in.add(read("?et2"));
            inputs.add(entityType2.ident);
        }
        // object type 2
        if (objectTypeUri2 != null) {
            where.add(read("(object-type ?e2 ?ot2)"));
            in.add(read("?ot2"));
            inputs.add(ident(objectTypeUri2));
        }
        //
        return queryRequest(find, in , where, inputs);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private QueryRequest queryRequest(List find, List in, List where, List inputs) {
        return queryRequest(map(
            read(":find"),  find,
            read(":in"),    in,
            read(":where"), where
        ), inputs.toArray());
    }

    private QueryRequest queryRequest(Object query, Object... inputs) {
        return QueryRequest.create(query, inputs);
    }
}
