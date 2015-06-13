package org.decaywood.service;

import org.apache.log4j.Logger;
import org.decaywood.dataAccess.UserDao;
import org.decaywood.entity.User;
import org.decaywood.exceptions.UserConflictException;
import org.decaywood.exceptions.ValueCantCastException;
import org.decaywood.utils.CommonUtils;
import org.decaywood.utils.ImageUtils;
import org.decaywood.utils.NameDomainMapper;
import org.decaywood.utils.TimeUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.Date;

/**
 * Created by decaywood on 2015/5/23.
 */

@Service("userService")
public class UserService {

    private static volatile String ROOT_PATH;
    Logger logger = Logger.getLogger(this.getClass().getName());

    @Resource(name = "userDataAccess")
    private UserDao dao;

    public void updateUserLastLoginTime(User user) {
        dao.updateUserLastLoginTime(user);
    }

    public User queryByUser(User user) {
        return dao.queryByUser(user);
    }

    public void registNewUser(User user, HttpServletRequest request) throws UserConflictException {

        User queryUser = dao.queryByUser(user);
        String errorInfo = null;

        if(queryUser != null){
            StringBuilder builder = new StringBuilder();
            String userEmail = queryUser.getUserEmail();
            String userName = queryUser.getUserName();
            String userLoginName = queryUser.getUserLoginName();


            if(userLoginName.equals(user.getUserLoginName()))
                builder.append("User Login Name Has Already Exist!");
            if(userName.equals(user.getUserName()))
                builder.append("User Name Has Already Exist!");
            if(userEmail.equals(user.getUserEmail()))
                builder.append("User Email Has Already Exist!");
            errorInfo = builder.toString();
        }

        if(errorInfo != null) throw new UserConflictException(errorInfo);

        userFormatPadding(user, request);
        dao.saveUser(user);

    }


    public String saveImage( InputStream stream,
                              String rootPath,
                              String fileType ) throws IOException, ValueCantCastException {
        String filePath = null;
        BufferedImage bufferedImage = ImageUtils.resizeImage(ImageIO.read(stream));

        try {
            String suffix = fileType.replace("image/", ".");
            String imgType = fileType.replace("image/", "");

            if(ROOT_PATH == null){
                File logoDirect = new File(rootPath + "userLogo");
                if(!logoDirect.exists() && !logoDirect.isDirectory()) logoDirect.mkdir();
                ROOT_PATH = logoDirect.getPath() + NameDomainMapper.BACK_SLASH.getName();
            }

            filePath = ROOT_PATH + ImageUtils.generateUUID(bufferedImage)+ suffix;

            File file = new File(filePath);

            if (!file.exists()) {
                ImageIO.write(bufferedImage, imgType, file);
                logger.debug(filePath);
            }

        } finally {
            bufferedImage.flush();
        }
        return filePath;
    }

    private void userFormatPadding(User user, HttpServletRequest request) {
        Date date = TimeUtils.getSqlTime();
        user.setUserID(CommonUtils.generateUUID())
            .setUserRole(NameDomainMapper.ROLE_USER.getName())
            .setUserLastLoginTime(date)
            .setUserRegisterTime(date)
            .setUserStatus(NameDomainMapper.STATUS_LOGIN.getName())
                .setUserIPAddress(request.getRemoteAddr());
    }

}
