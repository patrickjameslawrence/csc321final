package com.slowpoke.model.table.tablefile;

import com.slowpoke.connection.Connection;
import com.slowpoke.exception.DbException;
import com.slowpoke.model.DataBase;
import com.slowpoke.model.row.Row;
import com.slowpoke.model.table.TableDesc;
import com.slowpoke.model.page.Page;
import com.slowpoke.model.page.PageID;
import com.slowpoke.model.page.heap.HeapPage;
import com.slowpoke.model.page.heap.HeapPageID;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * file on disk
 *
 * @author zhangjw
 * @version 1.0
 */
public class HeapTableFile implements TableFile {

    private final File file;

    /**
     * table desc
     */
    private final TableDesc tableDesc;

    public HeapTableFile(File file, TableDesc tableDesc) {
        if (tableDesc == null) {
            throw new IllegalArgumentException("tableDesc cant not be null");
        }
        if (file == null) {
            throw new IllegalArgumentException("file cant not be null");
        }
        this.file = file;
        this.tableDesc = tableDesc;
    }

    @Override
    public TableDesc getTableDesc() {
        return tableDesc;
    }

    /**
     * read page from disk
     *
     * @param pageID pageID
     * @return page
     */
    @Override
    public Page readPageFromDisk(PageID pageID) {
        byte[] pageData = HeapPage.createEmptyPageData();
        try {
            FileInputStream in = new FileInputStream(file);
            in.skip(pageID.getPageNo() * DataBase.getDBConfig().getPageSizeInByte());
            in.read(pageData);
            return new HeapPage(pageID, pageData);
        } catch (IOException e) {
            throw new DbException("read Page from disk error", e);
        }
    }

    /**
     * write page to disk
     *
     * @param page
     */
    @Override
    public void writePageToDisk(Page page) {
        try {
            byte[] pgData = page.serialize();
            RandomAccessFile dbFile = new RandomAccessFile(file, "rws");
            dbFile.skipBytes(page.getPageID().getPageNo() * DataBase.getDBConfig().getPageSizeInByte());
            dbFile.write(pgData);
        } catch (IOException e) {
            throw new DbException("write page To disk error", e);
        }
    }

    /**
     * return table id
     */
    @Override
    public int getTableId() {
        return this.file.getAbsoluteFile().hashCode();
    }

    /**
     * existing page count
     */
    @Override
    public int getExistPageCount() {
        return (int) file.length() / DataBase.getDBConfig().getPageSizeInByte();
    }

    @Override
    public void insertRow(Row row) throws IOException {
        int existPageCount = this.getExistPageCount();
        HeapPage availablePage = (HeapPage) getFirstPageHasEmptySlot(existPageCount);

        // create a new page if no empty slot
        if (null == availablePage) {
            int pageNo = existPageCount; // page No. starts from 0
            PageID pageID = new HeapPageID(this.getTableId(), pageNo);
            availablePage = new HeapPage(pageID, HeapPage.createEmptyPageData());
            writePageToDisk(availablePage);
            availablePage = (HeapPage) DataBase.getBufferPool().getPage(pageID);
        }
        availablePage.insertRow(row);
        Connection.cacheDirtyPage(availablePage);
    }

    @Override
    public void deleteRow(Row row) {
        PageID pageID = row.getRowID().getPageID();
        HeapPage page = (HeapPage) DataBase.getBufferPool().getPage(pageID);
        page.deleteRow(row);
        Connection.cacheDirtyPage(page);
    }

    private Page getFirstPageHasEmptySlot(int existPageCount) throws IOException {
        if (existPageCount == 0) {
            return null;
        }

        for (int pageNo = 0; pageNo < existPageCount; pageNo++) {
            PageID pageID = new HeapPageID(this.getTableId(), pageNo);
            Page pg = DataBase.getBufferPool().getPage(pageID);
            if (pg.hasEmptySlot()) {
                return pg;
            }
        }
        return null;
    }

    @Override
    public ITableFileIterator getIterator() {
        return new HeapTableFileIterator();
    }

    //====================================Iterator=====================================

    private class HeapTableFileIterator implements ITableFileIterator {
        private Integer pageNo;
        private int tableId;
        private int existPageCount;
        private HeapPage curPage;
        private Iterator<Row> rowIterator;

        public HeapTableFileIterator() {
            this.pageNo = null;
            this.tableId = getTableId();
            this.existPageCount = getExistPageCount();
            this.curPage = null;
            this.rowIterator = null;
        }

        @Override
        public void open() throws DbException {
            pageNo = 0;
            curPage = getPage(pageNo);
            rowIterator = curPage.getRowIterator();
        }

        @Override
        public boolean hasNext() throws DbException {
            if (pageNo == null) {
                return false; // TableFileIterator not open
            }
            while (pageNo < existPageCount - 1) {
                if (rowIterator.hasNext()) {
                    return true;
                } else {
                    pageNo += 1;
                    curPage = getPage(pageNo);
                    rowIterator = curPage.getRowIterator();
                }
            }
            return rowIterator.hasNext();
        }

        @Override
        public Row next() throws DbException, NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException("no element ");
            }

            return rowIterator.next();
        }

        @Override
        public void close() {
            pageNo = null;
            rowIterator = null;
        }

        private HeapPage getPage(Integer pageNo) {
            PageID pageID = new HeapPageID(tableId, pageNo);
            return (HeapPage) DataBase.getBufferPool().getPage(pageID);
        }
        //
        // private Iterator<Row> getRowIterator(Integer pageNo) {
        //     PageID pageID = new HeapPageID(tableId, pageNo);
        //     return DataBase.getBufferPool().getPage(pageID).getRowIterator();
        // }
    }

}
