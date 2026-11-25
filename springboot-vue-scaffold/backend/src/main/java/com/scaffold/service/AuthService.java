package com.scaffold.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.common.BusinessException;
import com.scaffold.dto.AccountProfile;
import com.scaffold.dto.LoginRequest;
import com.scaffold.dto.LoginResponse;
import com.scaffold.dto.PasswordChangeRequest;
import com.scaffold.dto.ProfileUpdateRequest;
import com.scaffold.dto.RegisterRequest;
import com.scaffold.entity.Admin;
import com.scaffold.entity.User;
import com.scaffold.mapper.AdminMapper;
import com.scaffold.mapper.UserMapper;
import com.scaffold.security.JwtUtils;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    public static final String ACCOUNT_ADMIN = "ADMIN";
    public static final String ACCOUNT_USER = "USER";

    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private ISysLogService sysLogService;

    public LoginResponse login(LoginRequest request) {
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, request.getUsername())
                .last("LIMIT 1"));
        if (admin != null) {
            if (!admin.getPassword().equals(request.getPassword())) {
                throw new BusinessException("用户名或密码错误");
            }
            if (admin.getStatus() != null && admin.getStatus() == 0) {
                throw new BusinessException("账号已被禁用");
            }
            sysLogService.log(admin.getId(), admin.getUsername(), "LOGIN", "后台认证", "管理员登录", "登录成功");
            return buildAdminLoginResponse(admin);
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .last("LIMIT 1"));
        if (user == null || !user.getPassword().equals(request.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        sysLogService.log(user.getId(), user.getUsername(), "LOGIN", "用户认证", "用户登录", "登录成功");
        return buildUserLoginResponse(user);
    }

    public LoginResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        Long adminCount = adminMapper.selectCount(new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, username));
        Long userCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (adminCount > 0 || userCount > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(request.getPassword());
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : username);
        user.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null);
        user.setRole(ACCOUNT_USER);
        user.setStatus(1);
        userMapper.insert(user);

        sysLogService.log(user.getId(), user.getUsername(), "REGISTER", "用户认证", "用户注册", "注册成功");
        return buildUserLoginResponse(userMapper.selectById(user.getId()));
    }

    public AccountProfile getCurrentUser(Long accountId, String accountType) {
        if (ACCOUNT_ADMIN.equals(accountType)) {
            Admin admin = adminMapper.selectById(accountId);
            if (admin == null) {
                throw new BusinessException(404, "管理员不存在");
            }
            return toAdminProfile(admin);
        }
        User user = userMapper.selectById(accountId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return toUserProfile(user);
    }

    public AccountProfile updateProfile(Long accountId, String accountType, ProfileUpdateRequest request) {
        if (ACCOUNT_ADMIN.equals(accountType)) {
            Admin admin = adminMapper.selectById(accountId);
            if (admin == null) {
                throw new BusinessException(404, "管理员不存在");
            }
            if (StringUtils.hasText(request.getNickname())) {
                admin.setNickname(request.getNickname());
            }
            if (request.getAvatar() != null) {
                admin.setAvatar(request.getAvatar());
            }
            if (request.getPhone() != null) {
                admin.setPhone(request.getPhone());
            }
            adminMapper.updateById(admin);
            Admin saved = adminMapper.selectById(accountId);
            sysLogService.log(saved.getId(), saved.getUsername(), "OPERATION", "后台认证", "更新资料", "管理员更新个人资料");
            return toAdminProfile(saved);
        }

        User user = userMapper.selectById(accountId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        userMapper.updateById(user);
        User savedUser = userMapper.selectById(accountId);
        sysLogService.log(savedUser.getId(), savedUser.getUsername(), "OPERATION", "用户认证", "更新资料", "用户更新个人资料");
        return toUserProfile(savedUser);
    }

    public void changePassword(Long accountId, String accountType, PasswordChangeRequest request) {
        if (ACCOUNT_ADMIN.equals(accountType)) {
            Admin admin = adminMapper.selectById(accountId);
            if (admin == null) {
                throw new BusinessException(404, "管理员不存在");
            }
            if (!admin.getPassword().equals(request.getOldPassword())) {
                throw new BusinessException("原密码错误");
            }
            Admin patch = new Admin();
            patch.setId(accountId);
            patch.setPassword(request.getNewPassword());
            adminMapper.updateById(patch);
            sysLogService.log(admin.getId(), admin.getUsername(), "OPERATION", "后台认证", "修改密码", "管理员修改登录密码");
            return;
        }

        User user = userMapper.selectById(accountId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!user.getPassword().equals(request.getOldPassword())) {
            throw new BusinessException("原密码错误");
        }
        User patch = new User();
        patch.setId(accountId);
        patch.setPassword(request.getNewPassword());
        userMapper.updateById(patch);
        sysLogService.log(user.getId(), user.getUsername(), "OPERATION", "用户认证", "修改密码", "用户修改登录密码");
    }

    private LoginResponse buildAdminLoginResponse(Admin admin) {
        String token = jwtUtils.generateToken(admin.getId(), admin.getUsername(), ACCOUNT_ADMIN, ACCOUNT_ADMIN);
        return new LoginResponse(
                token,
                admin.getUsername(),
                admin.getNickname(),
                ACCOUNT_ADMIN,
                admin.getAvatar(),
                admin.getId(),
                ACCOUNT_ADMIN
        );
    }

    private LoginResponse buildUserLoginResponse(User user) {
        String role = StringUtils.hasText(user.getRole()) ? user.getRole() : ACCOUNT_USER;
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), role, ACCOUNT_USER);
        return new LoginResponse(
                token,
                user.getUsername(),
                user.getNickname(),
                role,
                user.getAvatar(),
                user.getId(),
                ACCOUNT_USER
        );
    }

    private AccountProfile toAdminProfile(Admin admin) {
        AccountProfile profile = new AccountProfile();
        profile.setId(admin.getId());
        profile.setUsername(admin.getUsername());
        profile.setNickname(admin.getNickname());
        profile.setAvatar(admin.getAvatar());
        profile.setPhone(admin.getPhone());
        profile.setRole(ACCOUNT_ADMIN);
        profile.setAccountType(ACCOUNT_ADMIN);
        profile.setStatus(admin.getStatus());
        profile.setCreatedAt(admin.getCreatedAt());
        return profile;
    }

    private AccountProfile toUserProfile(User user) {
        AccountProfile profile = new AccountProfile();
        profile.setId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setNickname(user.getNickname());
        profile.setAvatar(user.getAvatar());
        profile.setPhone(user.getPhone());
        profile.setRole(user.getRole());
        profile.setAccountType(ACCOUNT_USER);
        profile.setStatus(user.getStatus());
        profile.setCreatedAt(user.getCreatedAt());
        return profile;
    }
}
