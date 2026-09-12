package com.example.delivery.bitrix.ra;

import java.io.Serial;
import java.util.HashMap;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.IndexedRecord;
import jakarta.resource.cci.MappedRecord;
import jakarta.resource.cci.RecordFactory;

final class Bitrix24RecordFactory implements RecordFactory {

    @Override
    public <K, V> MappedRecord<K, V> createMappedRecord(String recordName) {
        return new Bitrix24MappedRecord<>(recordName);
    }

    @Override
    public <E> IndexedRecord<E> createIndexedRecord(String recordName)
            throws ResourceException {
        throw new NotSupportedException("Indexed records are not supported");
    }
}

final class Bitrix24MappedRecord<K, V> extends HashMap<K, V> implements MappedRecord<K, V> {

    @Serial
    private static final long serialVersionUID = 1L;

    private String recordName;
    private String recordShortDescription;

    Bitrix24MappedRecord(String recordName) {
        this.recordName = recordName;
    }

    @Override
    public String getRecordName() {
        return recordName;
    }

    @Override
    public void setRecordName(String recordName) {
        this.recordName = recordName;
    }

    @Override
    public String getRecordShortDescription() {
        return recordShortDescription;
    }

    @Override
    public void setRecordShortDescription(String recordShortDescription) {
        this.recordShortDescription = recordShortDescription;
    }

    @Override
    public Object clone() {
        Bitrix24MappedRecord<K, V> copy = new Bitrix24MappedRecord<>(recordName);
        copy.recordShortDescription = recordShortDescription;
        copy.putAll(this);
        return copy;
    }
}
