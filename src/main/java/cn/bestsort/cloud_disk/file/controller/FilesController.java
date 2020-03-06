package cn.bestsort.cloud_disk.file.controller;

import cn.bestsort.cloud_disk.file.entity.Files;
import cn.bestsort.cloud_disk.file.service.CloudDiskFileSystemInterface;
import cn.bestsort.cloud_disk.user.entity.User;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * (Files)表控制层
 *
 * @author bestsort
 * @since 2020-02-29 15:41:23
 */
@RestController
@RequestMapping("/files")
public class FilesController {
    /**
     * 服务对象
     */
    @Resource
    private CloudDiskFileSystemInterface fileInterface;
    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("selectOne")
    public Files selectOne(Long id) {
        return null;
    }

    @PostMapping("/copy")
    public String copyFileTo(String sourcePath,
                             String targetPath,
                             long fileId){
        return fileInterface.copyFileTo(sourcePath, targetPath, 1, 1, false);
    }
    @PostMapping("/rename")
    public boolean renameFile(String source,
                              String target,
                              HttpSession session,
                              long fileId){
        long userId = (long)session.getAttribute("userId");
        return fileInterface.renameFile(source, target, userId, fileId).getName().equals(target);
    }

    @PutMapping("/dir")
    public String mkDir(String path,
                         String name){
        return fileInterface.makeDir(path,name,1);
    }


    @PutMapping
    public boolean uploadFile(){
        return false;
    }

    @DeleteMapping
    public boolean deleteFile(long fileId,
                              long userId,
                              boolean isDir){
        return fileInterface.deleteFile(fileId,userId,isDir);
    }
}