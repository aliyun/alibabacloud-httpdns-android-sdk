package com.alibaba.sdk.android.httpdns.cache;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.net.NetworkStateManager;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author zonglin.nzl
 * @date 2020/12/11
 */
@RunWith(RobolectricTestRunner.class)
public class RecordDBHelperTest {

    private RecordDBHelper helper;
    private String accountId = RandomValue.randomStringWithFixedLength(8);

    @Before
    public void setUp() {
        NetworkStateManager.getInstance().init(RuntimeEnvironment.application);
        helper = new RecordDBHelper(RuntimeEnvironment.application, accountId);
    }

    @Test
    public void testSaveAndGet() {
        // 初始数据
        List<HostRecord> records = randomRecord(Constants.REGION_MAINLAND, 30);
        helper.insertOrUpdate(records);

        // 读取
        List<HostRecord> records3 = helper.readFromDb(Constants.REGION_MAINLAND);

        ArrayList<HostRecord> saved = new ArrayList<>();
        saved.addAll(records);

        ArrayList<HostRecord> read = new ArrayList<>(records3);

        assertRecordsEqual(saved, read);
    }

    @Test
    public void testDeleted() {
        // 初始数据
        List<HostRecord> records = randomRecord(Constants.REGION_MAINLAND, 30);
        helper.insertOrUpdate(records);

        // 删除一些
        ArrayList<HostRecord> left = new ArrayList<>(records);
        ArrayList<HostRecord> deleted = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            deleted.add(left.remove(RandomValue.randomInt(30 - i)));
        }
        helper.delete(deleted);

        // 读取
        List<HostRecord> records3 = helper.readFromDb(Constants.REGION_MAINLAND);

        ArrayList<HostRecord> saved = new ArrayList<>();
        saved.addAll(left);

        ArrayList<HostRecord> read = new ArrayList<>(records3);

        assertRecordsEqual(saved, read);
    }

    @Test
    public void testAdd() {
        // 初始数据
        List<HostRecord> records = randomRecord(Constants.REGION_MAINLAND, 30);
        helper.insertOrUpdate(records);

        // 新增一些
        List<HostRecord> records1 = randomRecord(Constants.REGION_MAINLAND, 10);
        helper.insertOrUpdate(records1);

        // 读取
        List<HostRecord> records3 = helper.readFromDb(Constants.REGION_MAINLAND);

        ArrayList<HostRecord> saved = new ArrayList<>();
        saved.addAll(records);
        saved.addAll(records1);

        ArrayList<HostRecord> read = new ArrayList<>(records3);

        assertRecordsEqual(saved, read);
    }

    @Test
    public void testUpdate() {
        // 初始数据
        List<HostRecord> records = randomRecord(Constants.REGION_MAINLAND, 30);
        helper.insertOrUpdate(records);

        // 新增一些
        List<HostRecord> records1 = randomRecord(Constants.REGION_MAINLAND, 10);
        helper.insertOrUpdate(records1);

        // 更新一些
        ArrayList<HostRecord> records2 = new ArrayList<>();
        for (HostRecord record : records1) {
            if (record.getType() == RequestIpType.v4.ordinal()) {
                record.setIps(RandomValue.randomIpv4s());
            } else {
                record.setIps(RandomValue.randomIpv6s());
            }
            record.setTtl(RandomValue.randomInt(300));
            records2.add(record);
        }
        helper.insertOrUpdate(records2);

        // 读取
        List<HostRecord> records3 = helper.readFromDb(Constants.REGION_MAINLAND);

        ArrayList<HostRecord> saved = new ArrayList<>();
        saved.addAll(records);
        saved.addAll(records2);

        ArrayList<HostRecord> read = new ArrayList<>(records3);

        assertRecordsEqual(saved, read);
    }

    @Test
    public void testCache() {
        // 初始数据
        List<HostRecord> records = randomRecord(Constants.REGION_MAINLAND, 30);
        helper.insertOrUpdate(records);

        // 删除一些
        ArrayList<HostRecord> left = new ArrayList<>(records);
        ArrayList<HostRecord> deleted = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            deleted.add(left.remove(RandomValue.randomInt(30 - i)));
        }
        helper.delete(deleted);

        // 新增一些
        List<HostRecord> records1 = randomRecord(Constants.REGION_MAINLAND, 10);
        helper.insertOrUpdate(records1);

        // 更新一些
        ArrayList<HostRecord> records2 = new ArrayList<>();
        for (HostRecord record : records1) {
            if (record.getType() == RequestIpType.v4.ordinal()) {
                record.setIps(RandomValue.randomIpv4s());
            } else {
                record.setIps(RandomValue.randomIpv6s());
            }
            record.setTtl(RandomValue.randomInt(300));
            records2.add(record);
        }
        helper.insertOrUpdate(records2);

        // 读取
        List<HostRecord> records3 = helper.readFromDb(Constants.REGION_MAINLAND);

        ArrayList<HostRecord> saved = new ArrayList<>();
        saved.addAll(left);
        saved.addAll(records2);

        ArrayList<HostRecord> read = new ArrayList<>(records3);

        assertRecordsEqual(saved, read);
    }


    @Test
    public void readCacheByRegion() {
        // 初始数据
        List<HostRecord> records = randomRecord(Constants.REGION_MAINLAND, 30);
        helper.insertOrUpdate(records);

        List<HostRecord> recordsInHK = randomRecord(Constants.REGION_HK, 30);
        helper.insertOrUpdate(recordsInHK);

        // 读取
        List<HostRecord> records3 = helper.readFromDb(Constants.REGION_MAINLAND);

        ArrayList<HostRecord> saved = new ArrayList<>();
        saved.addAll(records);

        ArrayList<HostRecord> read = new ArrayList<>(records3);

        assertRecordsEqual(saved, read);

        List<HostRecord> records4 = helper.readFromDb(Constants.REGION_HK);

        saved.clear();
        saved.addAll(recordsInHK);

        read.clear();
        read.addAll(records4);
        assertRecordsEqual(saved, read);

        List<HostRecord> records5 = helper.readFromDb(Constants.REGION_SG);

        MatcherAssert.assertThat("根据region读取缓存, sg的缓存应该为0", records5.size() == 0);
    }

    private void assertRecordsEqual(ArrayList<HostRecord> saved, ArrayList<HostRecord> read) {
        Collections.sort(saved, new Comparator<HostRecord>() {
            @Override
            public int compare(HostRecord o1, HostRecord o2) {
                return o1.hashCode() - o2.hashCode();
            }
        });
        Collections.sort(read, new Comparator<HostRecord>() {
            @Override
            public int compare(HostRecord o1, HostRecord o2) {
                return o1.hashCode() - o2.hashCode();
            }
        });

        MatcherAssert.assertThat("写入和读取的应该一样", read.size() == saved.size());
        for (int i = 0; i < read.size(); i++) {
            MatcherAssert.assertThat("写入和读取的应该一样", read.get(i), Matchers.is(saved.get(i)));
        }
    }


    public List<HostRecord> randomRecord(String region, int count) {
        ArrayList<HostRecord> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int style = RandomValue.randomInt(4);
            switch (style) {
                case 0:
                    list.add(HostRecord.create(region, RandomValue.randomHost(), RequestIpType.v4, RandomValue.randomJsonMap(), RandomValue.randomStringWithMaxLength(10), RandomValue.randomIpv4s(), RandomValue.randomInt(300)));
                    break;
                case 1:
                    list.add(HostRecord.create(region, RandomValue.randomHost(), RequestIpType.v4, null, null, RandomValue.randomIpv4s(), RandomValue.randomInt(300)));
                    break;
                case 2:
                    list.add(HostRecord.create(region, RandomValue.randomHost(), RequestIpType.v6, RandomValue.randomJsonMap(), RandomValue.randomStringWithMaxLength(10), RandomValue.randomIpv6s(), RandomValue.randomInt(300)));
                    break;
                default:
                    list.add(HostRecord.create(region, RandomValue.randomHost(), RequestIpType.v6, null, null, RandomValue.randomIpv6s(), RandomValue.randomInt(300)));
                    break;
            }
        }
        return list;
    }


}
