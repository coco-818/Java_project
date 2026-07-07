package com.example.dormmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

@SpringBootApplication
@EnableTransactionManagement // 开启声明式事务控制支持
public class DormManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(DormManagementApplication.class, args);
    }

    public static class SystemGuiLauncher {
        public static void main(String[] args) {
            // 1. 创建一个 Java GUI 桌面窗口
            JFrame frame = new JFrame("学生宿舍信息管理系统 - 桌面客户端");
            frame.setSize(450, 250);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null); // 窗口居中显示

            // 2. 设置布局面板
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout(20, 20));
            panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

            // 3. 添加一个标题文本组件（不加载任何图片，防止空指针）
            JLabel label = new JLabel("欢迎使用宿舍管理系统 (AI 大模型内核版)", JLabel.CENTER);
            label.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
            panel.add(label, BorderLayout.NORTH);

            // 4. 创建一个核心的 GUI 交互按钮
            JButton btn = new JButton("点击启动并进入系统控制台 (Web)");
            btn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
            btn.setBackground(new Color(64, 158, 255)); //
            btn.setForeground(Color.WHITE);

            // 5. 核心点击事件：点击 GUI 按钮时，自动用 Java 调用系统浏览器打开网页
            btn.addActionListener(e -> {
                try {
                    // 自动唤醒默认浏览器，打开系统主页
                    Desktop.getDesktop().browse(new URI("http://localhost:8080/index.html"));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "浏览器唤醒失败，请手动访问 http://localhost:8080");
                }
            });

            panel.add(btn, BorderLayout.CENTER);
            frame.add(panel);
            frame.setVisible(true); // GUI窗口显示出来
        }
    }
}

// 直接把全局跨域配置贴在启动类的下面（跟大类并列，不要写在大类里面）
@Configuration
class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*"); // 允许前端任何端口
        config.setAllowCredentials(true);    // 允许带上本地凭证(Cookie/localStorage)
        config.addAllowedMethod("*");        // 放行 GET/POST/PUT/DELETE
        config.addAllowedHeader("*");        // 放行所有请求头

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}