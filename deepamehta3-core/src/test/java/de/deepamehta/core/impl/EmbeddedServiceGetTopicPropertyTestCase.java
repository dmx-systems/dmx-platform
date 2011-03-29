package de.deepamehta.core.impl;

import de.deepamehta.core.model.PropValue;
import de.deepamehta.core.storage.Storage;
import de.deepamehta.core.storage.Transaction;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;



public class EmbeddedServiceGetTopicPropertyTestCase {

    private EmbeddedService cut;
    private Storage storageMock;
    private Transaction transactionMock;

    private long topicId = 1L;
    private String key = "key";
    private PropValue value = new PropValue("value");
    private RuntimeException throwable = new RuntimeException("error");

    @Before
    public void setup() {
        storageMock = createMock(Storage.class);
        transactionMock = createMock(Transaction.class);
        cut = new EmbeddedService(storageMock, null);
    }

    @Test
    public void ok() {
        // call expections
        expect(storageMock.beginTx()).andReturn(transactionMock);
        expect(storageMock.getTopicProperty(topicId, key)).andReturn(value);
        transactionMock.success();
        transactionMock.finish();
        //
        replay(storageMock, transactionMock);
        assertEquals(value, cut.getTopicProperty(topicId, key));
        verify(storageMock, transactionMock);
    }

    // includes property or topic not found (entity not found)
    @Test
    public void storageError() {
        // call expections
        expect(storageMock.beginTx()).andReturn(transactionMock);
        expect(storageMock.getTopicProperty(topicId, key)).andThrow(throwable);
        transactionMock.finish();
        //
        replay(storageMock, transactionMock);
        //
        try {
            cut.getTopicProperty(topicId, key);
        } catch (Exception e) {
            assertSame(throwable, e.getCause());
        }
        //
        verify(storageMock, transactionMock);
    }

    @Test
    public void beginTransactionError() {
        expect(storageMock.beginTx()).andThrow(throwable);
        replay(storageMock);
        //
        try {
            cut.getTopicProperty(1, "foo");
        } catch (Exception e) {
            assertSame(throwable, e);
        }
        //
        verify(storageMock);
    }
}
