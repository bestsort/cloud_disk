package cn.bestsort.cloud_disk.file.service.impl;

import cn.bestsort.cloud_disk.common.exception.ItemExtendException;
import cn.bestsort.cloud_disk.file.utils.FilePathUtil;
import cn.bestsort.cloud_disk.file.dao.FilesDao;
import cn.bestsort.cloud_disk.file.entity.Files;
import cn.bestsort.cloud_disk.file.service.ActualFileSystemInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.nio.channels.FileChannel;

/**
 * @author bestsort
 * @version 1.0
 * @date 2/29/20 3:01 PM
 */

@Slf4j
@Lazy
@Service("local")
public class LocalFileSystemImp implements ActualFileSystemInterface {
    @Resource
    private FilesDao fileDao;

    @Override
    public String makeDir(String path, String dirName, long userId) {
        File dir = new File(FilePathUtil.unionPath(FilePathUtil.getAbsolutePath(path),dirName));
        if(dir.mkdirs()){
            return path + dirName;
        };
        return null;
    }

    @Override
    public boolean deleteFile(long fileId, long userId, boolean isDir) {
        Files fileInfo = fileDao.queryById(fileId);
        if (fileInfo == null){
            throw new NullPointerException();
        }
        String path = FilePathUtil.tryAddPathEndCharset(fileInfo.getRealPath()) + fileInfo.getName();
        return tryDelete(new File(path));
    }
    /**
     * 递归删除目录下的所有文件及子目录下所有文件
     * @param dir 将要删除的文件目录
     * @return 删除结果
     */
    private boolean tryDelete(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            //递归删除目录中的子目录下
            if (children != null) {
                for (String child : children) {
                    boolean success = tryDelete(new File(dir, child));
                    if (!success) {
                        log.error("递归删除目录 [{}] 下文件失败", dir);
                        return false;
                    }
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }

    @Override
    public String copyFileTo(String sourcePath, String targetPath,long fileId, long userId, boolean isMoved) {
        try {
            Files fileInfo = fileDao.queryById(fileId);

            File sourcesFile = new File(FilePathUtil.unionAbsolutePath(fileInfo.getRealPath(),fileInfo.getName()));
            File targetFile = new File(FilePathUtil.unionAbsolutePath(targetPath, fileInfo.getName()));

            FileChannel source = new FileInputStream(sourcesFile).getChannel();
            FileChannel target = new FileOutputStream(targetFile).getChannel();
            try {
                target.transferFrom(source, 0, source.size());
                if (isMoved) {
                    sourcesFile.delete();
                }
            }finally {
                source.close();
                target.close();
            }
        } catch (IOException e) {
            log.error("用户{}尝试将文件 [{}] 移动到 [{}] 失败,原因:{}", userId, sourcePath, targetPath, e.getMessage());
            return null;
        }
        return targetPath;
    }

    @Override
    public Files renameFile(String sourceName, String targetName, long userId, long fileId) {
        log.info("用户(id:{}) 正尝试将文件(id:{}) [{}] 重命名为 [{}]",userId,fileId, sourceName, targetName);
        if (sourceName.equals(targetName)){
            return null;
        }
        Files fileInfo = fileDao.queryById(fileId);
        if (fileInfo == null){
            throw new NullPointerException("未查询到此文件信息, 请检查或刷新后重试");
        }
        if (!fileInfo.getName().equals(sourceName)){
            throw new IllegalArgumentException("请检查选中的文件, 或刷新后重试");
        }

        String path = FilePathUtil.tryAddPathEndCharset(fileInfo.getRealPath());
        File source = new File(path + fileInfo.getName());
        File target = new File(path + targetName);
        if (target.exists()){
            throw new ItemExtendException("该文件名已经存在");
        }

        //TODO 事务控制
        if (source.renameTo(target)){
            fileInfo.setName(targetName);
            fileDao.update(fileInfo);
            log.info("位于[{}]的文件 [{}] 被成功重命名, 新文件名为:[{}]", path, sourceName, targetName);
        }
        return fileInfo;
    }
}