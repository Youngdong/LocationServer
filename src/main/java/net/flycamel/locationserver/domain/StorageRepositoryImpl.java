package net.flycamel.locationserver.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.System.exit;
import static java.nio.file.StandardOpenOption.*;

@Repository
@Slf4j
public class StorageRepositoryImpl implements StorageRepository {
    private static final int NODE_LENGTH = 200;
    private static final List<LocationData> EMPTY_HISTORY = new ArrayList<>();

    @Value("${location.storage.fileName}")
    private String dbFileName;

    private ObjectMapper jsonMapper = new ObjectMapper();

    private AtomicInteger currentPosition;
    private FileChannel fc;
    ConcurrentHashMap<String, TreeSet<NodeInfo>> keyIndex;

    @PostConstruct
    public void init() {
        // 데이터 파일 오픈
        openDataFile();

        // 데이터를 끝까지 읽어서 키 인덱스 생성
        loadKeyIndex();
    }

    @Override
    public LocationData save(LocationData entity) {
        write(entity);
        return entity;
    }

    @Override
    public Optional<LocationData> findLastLocation(String key) {
        return Optional.ofNullable(keyIndex.get(key).last())
                .map(node -> readOneData(node.getPos()))
                .orElse(null);
    }

    @Override
    public List<LocationData> findHistory(String key, long startTime, long endTime) {
        return Optional.ofNullable(keyIndex.get(key))
                .map(nodeInfoSet -> nodeInfoSet.subSet(new NodeInfo(startTime, 0), new NodeInfo(endTime, 0))
                        .stream()
                        .map(node -> readOneData(node.getPos()).orElse(null))
                        .collect(Collectors.toList()))
                .orElse(EMPTY_HISTORY);
    }

    @Override
    public List<LocationData> getAllLastLocation() {
        return keyIndex.values().parallelStream()
                .unordered()
                .map(TreeSet::last)
                .map(node -> readOneData(node.getPos()).orElse(null))
                .collect(Collectors.toList());
    }

    private void openDataFile() {
        try {
            fc = FileChannel.open(Paths.get(dbFileName), READ, WRITE, CREATE);
        } catch (IOException e) {
            log.error("Cannot open db file : {}", dbFileName, e);
            exit(1);
        }
    }

    private void loadKeyIndex() {
        ConcurrentHashMap<String, TreeSet<NodeInfo>> indexMap = new ConcurrentHashMap<>();
        boolean fullLoaded = false;
        int pos = 0;

        while (!fullLoaded) {

            LocationData entity = null;
            try {
                entity = readData(pos);
                addIndex(entity, pos, indexMap);

                pos++;
            } catch (EOFException e) {
                log.info("Data Fully Loaded, {}", indexMap.toString());
                fullLoaded = true;
            }
        }

        this.currentPosition = new AtomicInteger(pos);
        this.keyIndex = indexMap;

        log.info("Initial currentPosition : {}", currentPosition.get());
        log.info("Initial keyIndex : {}", keyIndex.toString());
    }

    private ConcurrentHashMap<String, TreeSet<NodeInfo>> addIndex(LocationData entity, int pos) {
        return addIndex(entity, pos, keyIndex);
    }

    private ConcurrentHashMap<String, TreeSet<NodeInfo>> addIndex(LocationData entity, int pos, ConcurrentHashMap<String, TreeSet<NodeInfo>> index) {
        index.compute(entity.getKey(), (key, value) -> {
            if (null == value) {
                TreeSet<NodeInfo> treeSet = new TreeSet<>();
                treeSet.add(new NodeInfo(entity.getSecondKey(), pos));

                return treeSet;
            } else {
                value.add(new NodeInfo(entity.getSecondKey(), pos));

                return value;
            }
        });

        return index;
    }

    private LocationData readData(int pos) throws EOFException {
        ByteBuffer buffer;

        try {
            buffer = ByteBuffer.allocate(NODE_LENGTH);
            int readCount = fc.read(buffer, filePosition(pos));
            if (-1 == readCount) {
                throw new EOFException("eof");
            }

            LocationData entity = fromByteBuffer(buffer);
            log.debug("Read Entity : {}", entity);

            return entity;
        } catch (EOFException eofe) {
            log.info("End of file");
            throw eofe;
        } catch (IOException e) {
            log.error("Cannot read data.", e);
            exit(3);
        }

        return null;
    }

    private Optional<LocationData> readOneData(int pos) {
        ByteBuffer buffer;

        try {
            buffer = ByteBuffer.allocate(NODE_LENGTH);
            fc.read(buffer, filePosition(pos));

            LocationData entity = fromByteBuffer(buffer);
            log.debug("Read Entity : {}", entity);

            return Optional.ofNullable(entity);
        } catch (IOException e) {
            log.error("Cannot read data.", e);
            exit(3);
        }

        return Optional.empty();
    }

    private LocationData fromByteBuffer(ByteBuffer buffer) {
        String nodeString = new String(buffer.array());
        log.debug("Read Data String : {}", nodeString);

        try {
            return jsonMapper.readValue(nodeString.trim(), LocationData.class);
        } catch (IOException e) {
            log.error("Cannot read json : {}", nodeString, e);
        }

        return null;
    }

    private synchronized void write(LocationData entity) {
        writeTo(entity, currentPosition.get());
        addIndex(entity, currentPosition.getAndIncrement());
    }

    private void writeTo(LocationData entity, int pos) {
        ByteBuffer buffer;

        try {
            String jsonEntity = jsonMapper.writeValueAsString(entity);
            buffer = ByteBuffer.wrap(jsonEntity.getBytes());

            fc.write(buffer, filePosition(pos));

        } catch (IOException e) {
            log.error("Cannot write data.", e);
            exit(4);
        }
    }

    private long filePosition(int pos) {
        return (long) pos * NODE_LENGTH;
    }

    @lombok.Value
    private class NodeInfo implements Comparable<NodeInfo> {
        Long secondKey;
        int pos;

        @Override
        public int compareTo(NodeInfo o) {
            return this.secondKey.compareTo(o.secondKey);
        }
    }
}
