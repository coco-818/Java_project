package com.example.dormmanagement; //

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SystemGuiLauncher extends JFrame {

    private final String BASE_URL = "http://localhost:8080";

    // 全局当前登录用户信息
    private String currentUsername = "";
    private String currentUserRole = ""; // "ADMIN" 或 "USER"

    // 核心 UI 组件
    private JTextField txtStudentId, txtName, txtDormId;
    private JTable tableStudents, tableMyApply, tableAudit;
    private DefaultTableModel modelStudents, modelMyApply, modelAudit;
    private JTextArea aiReportArea;
    private JTabbedPane tabbedPane; // 多功能选项卡

    public SystemGuiLauncher() {
        // 先强行触发真正的“账号密码登录框”
        if (!showLoginDialog()) {
            System.exit(0);
        }

        // 登录成功后，根据角色初始化主界面
        setTitle("学生宿舍管理系统 - 桌面 AI 客户端 (当前用户: " + currentUsername + " [" + currentUserRole + "])");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}

        // 创建多功能选项卡面板
        tabbedPane = new JTabbedPane();

        //  模块 1：学生住宿档案中心 (所有人可见)
        initStudentModule();

        //  模块 2：调宿中心 (根据角色，学生显示申请，管理员显示审批)
        if ("ADMIN".equals(currentUserRole)) {
            initAuditModule(); // 管理员审批面板
        } else {
            initApplyModule(); // 学生申请面板
        }

        add(tabbedPane, BorderLayout.CENTER);
        setVisible(true);
    }

    //  1. 登录/注册交互框
    private boolean showLoginDialog() {
        JDialog loginDialog = new JDialog((Frame) null, "系统身份认证中心", true);
        loginDialog.setSize(350, 220);
        loginDialog.setLocationRelativeTo(null);
        loginDialog.setLayout(new GridLayout(4, 2, 10, 10));

        JTextField txtUser = new JTextField();
        JPasswordField txtPass = new JPasswordField();

        loginDialog.add(new JLabel("  用户名 / 学号:")); loginDialog.add(txtUser);
        loginDialog.add(new JLabel("  系统密码:")); loginDialog.add(txtPass);

        JButton btnLogin = new JButton("安全登录");
        JButton btnRegister = new JButton("快捷注册");

        // 登录按钮核心事件：走后端的 /user/login 接口
        btnLogin.addActionListener(e -> {
            String username = txtUser.getText();
            String password = new String(txtPass.getPassword());
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(loginDialog, "请输入完整的账号和密码！");
                return;
            }
            try {
                String json = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
                URL url = new URL(BASE_URL + "/user/login");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) { os.write(json.getBytes(StandardCharsets.UTF_8)); }

                if (conn.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder(); String line;
                    while ((line = in.readLine()) != null) sb.append(line);
                    in.close();

                    String raw = sb.toString();
                    if (raw.contains("\"code\":200")) {
                        // 登录成功，提取角色域
                        this.currentUsername = username;
                        // 简单判断是否为 admin，或者提取 role 字段
                        this.currentUserRole = raw.contains("\"role\":\"ADMIN\"") || "admin".equalsIgnoreCase(username) ? "ADMIN" : "USER";
                        JOptionPane.showMessageDialog(loginDialog, "身份验证成功，欢迎进入系统！");
                        loginDialog.dispose();
                    } else {
                        String msg = fetchField(raw, "msg");
                        JOptionPane.showMessageDialog(loginDialog, "登录失败: " + (msg.isEmpty() ? "密码错误" : msg));
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(loginDialog, "无法连接到后端服务器！");
            }
        });

        // 注册按钮事件：走后端的 /user/register 接口
        btnRegister.addActionListener(e -> {
            String username = txtUser.getText();
            String password = new String(txtPass.getPassword());
            if (username.isEmpty() || password.isEmpty()) { JOptionPane.showMessageDialog(loginDialog, "请先填写要注册的账号密码"); return; }
            try {
                String json = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
                URL url = new URL(BASE_URL + "/user/register");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) { os.write(json.getBytes(StandardCharsets.UTF_8)); }
                if (conn.getResponseCode() == 200) {
                    JOptionPane.showMessageDialog(loginDialog, "注册提交成功，请点击登录！");
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(loginDialog, "注册失败！"); }
        });

        loginDialog.add(btnLogin); loginDialog.add(btnRegister);
        loginDialog.setVisible(true);
        return !this.currentUsername.isEmpty();
    }

    // 2. 学生档案与高频排序模块
    private void initStudentModule() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        // 搜索控制台
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        searchPanel.setBorder(BorderFactory.createTitledBorder("多条件筛选与动态排序控制台"));

        searchPanel.add(new JLabel("学号:")); txtStudentId = new JTextField(8); searchPanel.add(txtStudentId);
        searchPanel.add(new JLabel("姓名:")); txtName = new JTextField(6); searchPanel.add(txtName);
        searchPanel.add(new JLabel("宿舍:")); txtDormId = new JTextField(5); searchPanel.add(txtDormId);

        JButton btnSearch = new JButton("筛选");
        JButton btnReset = new JButton("重置");
        JButton btnSortDept = new JButton("按院系排序");
        JButton btnSortClass = new JButton("按班级排序");
        JButton btnAdd = new JButton("新增登记");
        JButton btnAI = new JButton("AI 智能评估");

        // 红色的退出/切换账号按钮
        JButton btnLogout = new JButton("切换账号/退出");
        btnLogout.setBackground(new Color(245, 108, 108)); // 警示红色
        btnLogout.setForeground(Color.RED); // 强行让文字显色，防止隐形

        // 强行把文字全部改成纯黑色，防止在某些电脑主题上隐形
        for (JButton b : new JButton[]{btnSearch, btnReset, btnSortDept, btnSortClass, btnAdd, btnAI, btnLogout}) {
            if (b != btnLogout) b.setForeground(Color.BLACK);
            searchPanel.add(b);
        }

        // 权限隔离
        if (!"ADMIN".equals(currentUserRole)) {
            btnAdd.setEnabled(false); btnAI.setEnabled(false);
        }

        panel.add(searchPanel, BorderLayout.NORTH);

        // 表格展示
        String[] columns = {"学号", "姓名", "院系", "班级", "宿舍号", "床位号", "联系方式"};
        modelStudents = new DefaultTableModel(columns, 0);
        tableStudents = new JTable(modelStudents);
        panel.add(new JScrollPane(tableStudents), BorderLayout.CENTER);

        // AI决策板
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createTitledBorder("DeepSeek 大模型智能化决策看板"));
        aiReportArea = new JTextArea(5, 20);
        aiReportArea.setLineWrap(true); aiReportArea.setEditable(false);
        aiReportArea.setBackground(new Color(240, 249, 235));
        bottomPanel.add(new JScrollPane(aiReportArea), BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        // 事件绑定
        btnSearch.addActionListener(e -> refreshStudentList(null));
        btnReset.addActionListener(e -> { txtStudentId.setText(""); txtName.setText(""); txtDormId.setText(""); refreshStudentList(null); });
        btnSortDept.addActionListener(e -> refreshStudentList("dept"));
        btnSortClass.addActionListener(e -> refreshStudentList("class"));
        btnAdd.addActionListener(e -> openAddStudentDialog());
        btnAI.addActionListener(e -> triggerAIAnalysis());

        //  绑定注销退出按钮的核心事件
        btnLogout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "确定要注销当前登录，切换其它账号吗？", "提示", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                this.dispose();
                new SystemGuiLauncher();
            }
        });

        if ("ADMIN".equals(currentUserRole)) {
            JPopupMenu pm = new JPopupMenu(); JMenuItem di = new JMenuItem("物理删除该学生记录");
            pm.add(di); tableStudents.setComponentPopupMenu(pm);
            di.addActionListener(e -> performDeleteStudent());
        }

        refreshStudentList(null);
        tabbedPane.addTab("学生住宿档案中心", panel);
    }

    private void refreshStudentList(String sortBy) {
        new Thread(() -> {
            try {
                // 根据是否点击了排序，切换不同的路由端点
                String targetUrl = BASE_URL + (sortBy == null ? "/student/list" : "/student/list/sort?sortBy=" + sortBy);
                URL url = new URL(targetUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (conn.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder(); String line;
                    while ((line = in.readLine()) != null) sb.append(line); in.close();

                    String raw = sb.toString();
                    SwingUtilities.invokeLater(() -> {
                        modelStudents.setRowCount(0);
                        String[] items = raw.split("\\},\\{");
                        for (String item : items) {
                            String sId = fetchField(item, "studentId");
                            String name = fetchField(item, "name");
                            String dept = fetchField(item, "department");
                            String clazz = fetchField(item, "className");
                            String dId = fetchField(item, "dormId");
                            String bNum = fetchField(item, "bedNumber");
                            String phone = fetchField(item, "phone");

                            if (!txtStudentId.getText().isEmpty() && !sId.contains(txtStudentId.getText())) continue;
                            if (!txtName.getText().isEmpty() && !name.contains(txtName.getText())) continue;
                            if (!txtDormId.getText().isEmpty() && !dId.contains(txtDormId.getText())) continue;

                            if (!sId.isEmpty()) modelStudents.addRow(new Object[]{sId, name, dept, clazz, dId, bNum, phone});
                        }
                    });
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    //  3. 普通用户的“调宿申请”模块
    private void initApplyModule() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 填写表单区
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("提交在线调宿申请书"));
        JTextField txtTargetDorm = new JTextField();
        JTextField txtReason = new JTextField();

        formPanel.add(new JLabel("目标宿舍号:")); formPanel.add(txtTargetDorm);
        formPanel.add(new JLabel("调宿申请缘由:")); formPanel.add(txtReason);
        JButton btnSubmitApply = new JButton("安全提请审批");
        btnSubmitApply.setForeground(Color.BLACK);
        formPanel.add(new JLabel()); formPanel.add(btnSubmitApply);
        panel.add(formPanel, BorderLayout.NORTH);

        // 历史申请表格
        String[] cols = {"当前宿舍", "目标宿舍", "调宿理由", "当前审批状态"};
        modelMyApply = new DefaultTableModel(cols, 0);
        tableMyApply = new JTable(modelMyApply);
        JScrollPane sp = new JScrollPane(tableMyApply);
        sp.setBorder(BorderFactory.createTitledBorder("我的调宿申请流转记录"));
        panel.add(sp, BorderLayout.CENTER);

        // 提交事件
        btnSubmitApply.addActionListener(e -> {
            if (txtTargetDorm.getText().isEmpty()) { JOptionPane.showMessageDialog(this, "请填写目标宿舍！"); return; }
            try {
                String json = String.format("{\"studentId\":\"%s\",\"targetDormId\":\"%s\",\"reason\":\"%s\"}",
                        currentUsername, txtTargetDorm.getText(), txtReason.getText());
                URL url = new URL(BASE_URL + "/exchange/apply");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) { os.write(json.getBytes(StandardCharsets.UTF_8)); }
                if (conn.getResponseCode() == 200) {
                    JOptionPane.showMessageDialog(this, "申请已送达后勤审批中心！");
                    txtTargetDorm.setText(""); txtReason.setText("");
                    refreshMyApplyList();
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "提交申请失败！"); }
        });

        refreshMyApplyList();
        tabbedPane.addTab("个人调宿申请中心", panel);
    }

    private void refreshMyApplyList() {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/exchange/list");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (conn.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder(); String line;
                    while ((line = in.readLine()) != null) sb.append(line); in.close();

                    String raw = sb.toString();
                    SwingUtilities.invokeLater(() -> {
                        modelMyApply.setRowCount(0);
                        String[] items = raw.split("\\},\\{");
                        for (String item : items) {
                            String sId = fetchField(item, "studentId");
                            // 核心：过滤只看当前登录学号的申请
                            if (!sId.equals(currentUsername)) continue;
                            String curDorm = fetchField(item, "currentDormId");
                            String tgtDorm = fetchField(item, "targetDormId");
                            String reason = fetchField(item, "reason");
                            String status = fetchField(item, "status");
                            modelMyApply.addRow(new Object[]{curDorm, tgtDorm, reason, status});
                        }
                    });
                }
            } catch (Exception ex) {}
        }).start();
    }

    //  4. 管理员的“调宿审批”模块
    private void initAuditModule() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        String[] cols = {"流水单号ID", "申请学号", "当前宿舍", "目标宿舍", "理由", "当前状态"};
        modelAudit = new DefaultTableModel(cols, 0);
        tableAudit = new JTable(modelAudit);
        panel.add(new JScrollPane(tableAudit), BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnApprove = new JButton("批准调宿 (事务回滚安全型)");
        JButton btnReject = new JButton("无情拒绝");
        btnApprove.setForeground(Color.BLACK); btnReject.setForeground(Color.BLACK);

        actionPanel.add(btnApprove); actionPanel.add(btnReject);
        panel.add(actionPanel, BorderLayout.SOUTH);

        // 批准核心处理流：走后端的 PUT /exchange/audit 接口
        btnApprove.addActionListener(e -> performAuditAction("APPROVED"));
        btnReject.addActionListener(e -> performAuditAction("REJECTED"));

        refreshAllAuditList();
        tabbedPane.addTab("后勤调宿审批中心 (ADMIN)", panel);
    }

    private void refreshAllAuditList() {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/exchange/list");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (conn.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder(); String line;
                    while ((line = in.readLine()) != null) sb.append(line); in.close();

                    String raw = sb.toString();
                    SwingUtilities.invokeLater(() -> {
                        modelAudit.setRowCount(0);
                        String[] items = raw.split("\\},\\{");
                        for (String item : items) {
                            String id = fetchField(item, "id");
                            String sId = fetchField(item, "studentId");
                            String curDorm = fetchField(item, "currentDormId");
                            String tgtDorm = fetchField(item, "targetDormId");
                            String reason = fetchField(item, "reason");
                            String status = fetchField(item, "status");
                            if (!id.isEmpty()) modelAudit.addRow(new Object[]{id, sId, curDorm, tgtDorm, reason, status});
                        }
                    });
                }
            } catch (Exception ex) {}
        }).start();
    }

    private void performAuditAction(String status) {
        int row = tableAudit.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "请先鼠标左键选中一行审批单记录！"); return; }
        String id = tableAudit.getValueAt(row, 0).toString();
        String currentStatus = tableAudit.getValueAt(row, 5).toString();
        if (!"PENDING".equals(currentStatus)) { JOptionPane.showMessageDialog(this, "该申请单已处理完毕，请勿重复操作！"); return; }

        new Thread(() -> {
            try {
                // 对应原前端 axios.put("/exchange/audit", {id, status})
                String json = String.format("{\"id\":%s,\"status\":\"%s\"}", id, status);
                URL url = new URL(BASE_URL + "/exchange/audit");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) { os.write(json.getBytes(StandardCharsets.UTF_8)); }

                if (conn.getResponseCode() == 200) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "审批决策成功，联动事务已原子性下发！");
                        refreshAllAuditList();
                        refreshStudentList(null); // 同步刷新学生住宿表
                    });
                }
            } catch (Exception ex) { SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "审批操作失败！")); }
        }).start();
    }

    //  其它辅助旧有业务逻辑：新增、删除、AI 分析
    private void openAddStudentDialog() {
        JDialog dialog = new JDialog(this, "快捷录入新入住学生", true);
        dialog.setSize(350, 400); dialog.setLocationRelativeTo(this); dialog.setLayout(new GridLayout(8, 2, 10, 10));
        JTextField jId = new JTextField(); JTextField jName = new JTextField(); JTextField jDept = new JTextField();
        JTextField jClass = new JTextField(); JTextField jDorm = new JTextField(); JTextField jBed = new JTextField();
        JTextField jPhone = new JTextField();
        dialog.add(new JLabel("  学号:")); dialog.add(jId); dialog.add(new JLabel("  姓名:")); dialog.add(jName);
        dialog.add(new JLabel("  院系:")); dialog.add(jDept); dialog.add(new JLabel("  班级:")); dialog.add(jClass);
        dialog.add(new JLabel("  宿舍号:")); dialog.add(jDorm); dialog.add(new JLabel("  床位号:")); dialog.add(jBed);
        dialog.add(new JLabel("  联系方式:")); dialog.add(jPhone);

        JButton btnSubmit = new JButton("确认提交入库"); btnSubmit.setForeground(Color.BLACK);
        btnSubmit.addActionListener(e -> {
            String json = String.format("{\"studentId\":\"%s\",\"name\":\"%s\",\"department\":\"%s\",\"className\":\"%s\",\"dormId\":\"%s\",\"bedNumber\":\"%s\",\"phone\":\"%s\"}",
                    jId.getText(), jName.getText(), jDept.getText(), jClass.getText(), jDorm.getText(), jBed.getText(), jPhone.getText());
            new Thread(() -> {
                try {
                    URL url = new URL(BASE_URL + "/student/add");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    try (OutputStream os = conn.getOutputStream()) { os.write(json.getBytes(StandardCharsets.UTF_8)); }
                    int responseCode = conn.getResponseCode();
                    // 无论后端是成功（200）还是失败拦截（500），都把后端的 JSON 响应读取出来
                    if (responseCode == 200 || responseCode == 500) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(
                                responseCode == 200 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder(); String line;
                        while ((line = in.readLine()) != null) sb.append(line); in.close();

                        String raw = sb.toString();
                        String serverMsg = fetchField(raw, "msg"); // 抓取后端 Result 里的 msg 提示信息

                        SwingUtilities.invokeLater(() -> {
                            if (raw.contains("\"code\":200")) {
                                JOptionPane.showMessageDialog(dialog, "学生登记成功！");
                                dialog.dispose();
                                refreshStudentList(null);
                            } else {
                                //如果床位被占用了，这里会自动弹窗拦截：“录入受阻：【床位冲突警告】...”
                                JOptionPane.showMessageDialog(dialog, "录入受阻：" + (serverMsg.isEmpty() ? "床位冲突或冲突检测生效" : serverMsg));
                            }
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(dialog, "后端响应异常，状态码：" + responseCode));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(dialog, "无法连接到后端，请确认 Spring Boot 服务已启动！"));
                }
            }).start();
        });
        dialog.add(new JLabel()); dialog.add(btnSubmit); dialog.setVisible(true);
    }

    private void performDeleteStudent() {
        int row = tableStudents.getSelectedRow();
        if (row == -1) return;
        String studentId = tableStudents.getValueAt(row, 0).toString();
        int confirm = JOptionPane.showConfirmDialog(this, "确定销户吗？", "警告", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                try {
                    URL url = new URL(BASE_URL + "/student/delete/" + studentId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection(); conn.setRequestMethod("DELETE");
                    if (conn.getResponseCode() == 200) SwingUtilities.invokeLater(() -> { refreshStudentList(null); });
                } catch (Exception ex) {}
            }).start();
        }
    }

    private void triggerAIAnalysis() {
        aiReportArea.setText("【大模型提示】正在接入 DeepSeek-Chat 决策引擎分析中，请稍候...");
        String queryDormId = txtDormId.getText();
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/analysis/ai?dormId=" + queryDormId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (conn.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder(); String line;
                    while ((line = in.readLine()) != null) sb.append(line); in.close();
                    String formattedText = fetchField(sb.toString(), "data").replace("\\n", "\n").replace("\\r", "");
                    SwingUtilities.invokeLater(() -> aiReportArea.setText(formattedText));
                }
            } catch (Exception ex) { SwingUtilities.invokeLater(() -> aiReportArea.setText("AI分析连接失败。")); }
        }).start();
    }

    private String fetchField(String target, String fieldName) {
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + fieldName + "\":\\s*\"?([^\",\\}]+)\"?");
            java.util.regex.Matcher m = p.matcher(target);
            if (m.find()) return m.group(1);
        } catch (Exception e) {}
        return "";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SystemGuiLauncher::new);
    }
}