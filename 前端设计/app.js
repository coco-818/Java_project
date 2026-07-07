axios.defaults.baseURL = "http://localhost:8080";
axios.defaults.headers.post["Content-Type"] = "application/json";
axios.defaults.withCredentials = true;

// 1. 宿舍学生管理
// 宿舍学生管理
// 宿舍学生管理
// 宿舍学生管理
const Dorm = {
    data() {
        return {
            list: [],
            query: {studentId: "", name: "", dormId: ""}, // 🟢 统一改为 dormId
            form: {},
            dialogVisible: false,
            aiReportText: ""
        }
    },
    mounted() {
        this.getList();
    },
    methods: {
        // 获取学生列表 GET
        async getList() {
            try {
                const res = await axios.get("/student/list");
                let data = res.data.data || [];
                // 前端条件搜索
                if (this.query.studentId) {
                    data = data.filter(item => item.studentId.includes(this.query.studentId));
                }
                if (this.query.name) {
                    data = data.filter(item => item.name.includes(this.query.name));
                }
                // 🟢 修复这里的模糊查询字段：改为 item.dormId 和 query.dormId
                if (this.query.dormId) {
                    data = data.filter(item => item.dormId && item.dormId.includes(this.query.dormId));
                }
                this.list = data;
            } catch (e) {
                this.$message.error("获取数据失败");
            }
        },
        search() {
            this.getList();
        },
        reset() {
            this.query = {studentId: "", name: "", dormId: ""}; // 🟢 统一重置为 dormId
            this.getList();
        },
        add() {
            this.dialogVisible = true;
            this.form = {};
        },
        // 保存学生 POST
        async save() {
            try {
                await axios.post("/student/add", this.form);
                this.$message.success("保存成功");
                this.dialogVisible = false;
                this.getList();
            } catch (e) {
                this.$message.error("保存失败");
            }
        },
        // 删除学生 DELETE
        async del(studentId) {
            try {
                await axios.delete("/student/delete/" + studentId);
                this.$message.success("删除成功");
                this.getList();
            } catch (e) {
                this.$message.error("删除失败");
            }
        },
        async changeSort(type) {
            try {
                const res = await axios.get("/student/list/sort?sortBy=" + type);
                this.list = res.data.data || [];
                this.$message ? this.$message.success("排序完成") : alert("排序完成");
            } catch (e) {
                this.$message ? this.$message.error("排序失败") : alert("排序失败");
            }
        },
        async getAIReport() {
            try {
                this.aiReportText = "大模型正在疯狂计算、分析宿舍数据中，请稍候...";
                if (this.$message) this.$message.info("大模型分析中...");
                // 🟢 智能分析：把当前搜索框的宿舍号传给后端
                const currentDormId = this.query.dormId || "";
                const res = await axios.get("/analysis/ai?dormId=" + currentDormId);
                this.aiReportText = res.data.data;
            } catch (e) {
                this.aiReportText = "大模型接口连接失败，请检查后端服务是否开启。";
            }
        }
    },
    template: `
<div>
    <h3>学生住宿管理</h3>
    <el-form :inline="true" class="demo-form-inline">
        <el-form-item label="学号">
            <el-input v-model="query.studentId" placeholder="学号查询"></el-input>
        </el-form-item>
        <el-form-item label="姓名">
            <el-input v-model="query.name" placeholder="姓名查询"></el-input>
        </el-form-item>
        <el-form-item label="宿舍号">
            <el-input v-model="query.dormId" placeholder="宿舍号"></el-input>
        </el-form-item>
        <el-form-item>
            <el-button type="primary" @click="search">搜索</el-button>
            <el-button @click="reset">重置</el-button>
            <el-button type="info" @click="changeSort('dept')">按院系排序</el-button>
            <el-button type="info" @click="changeSort('class')">按班级排序</el-button>
            
            <el-button type="success" @click="add" v-if="$root.user.username === 'admin'">新增</el-button>
            <el-button type="warning" @click="getAIReport" v-if="$root.user.username === 'admin'">AI 智能分析评估</el-button>
        </el-form-item>
    </el-form>

    <div v-if="aiReportText" style="margin: 15px 0; padding: 15px; background-color: #f0f9eb; border-left: 5px solid #67c23a; white-space: pre-line; color: #606266; font-size: 14px;">
        {{ aiReportText }}
    </div>

    <el-table :data="list" border style="margin-top:10px">
        <el-table-column prop="studentId" label="学号"></el-table-column>
        <el-table-column prop="name" label="姓名"></el-table-column>
        <el-table-column prop="department" label="院系"></el-table-column>
        <el-table-column prop="className" label="班级"></el-table-column>
        <el-table-column prop="dormId" label="宿舍号"></el-table-column>
        <el-table-column prop="bedNumber" label="床位号"></el-table-column>
        <el-table-column prop="phone" label="联系方式"></el-table-column>
        <el-table-column label="操作" v-if="$root.user.username === 'admin'">
            <template slot-scope="scope">
                <el-button type="danger" icon="el-icon-delete" size="mini" @click="del(scope.row.studentId)">删除</el-button>
            </template>
        </el-table-column>
    </el-table>

    <!-- 🟢 补齐缺失的新增学生弹窗（核心问题所在） -->
    <el-dialog title="新增学生住宿登记" :visible.sync="dialogVisible" width="500px">
        <el-form :model="form" label-width="100px">
            <el-form-item label="学号">
                <el-input v-model="form.studentId" placeholder="请输入学号"></el-input>
            </el-form-item>
            <el-form-item label="姓名">
                <el-input v-model="form.name" placeholder="请输入姓名"></el-input>
            </el-form-item>
            <el-form-item label="院系">
                <el-input v-model="form.department" placeholder="请输入院系"></el-input>
            </el-form-item>
            <el-form-item label="班级">
                <el-input v-model="form.className" placeholder="请输入班级"></el-input>
            </el-form-item>
            <el-form-item label="宿舍号">
                <el-input v-model="form.dormId" placeholder="请输入宿舍号"></el-input>
            </el-form-item>
            <el-form-item label="床位号">
                <el-input v-model="form.bedNumber" placeholder="请输入床位号（如1-4）"></el-input>
            </el-form-item>
            <el-form-item label="联系方式">
                <el-input v-model="form.phone" placeholder="请输入手机号"></el-input>
            </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
            <el-button @click="dialogVisible = false">取 消</el-button>
            <el-button type="primary" @click="save">确 定</el-button>
        </div>
    </el-dialog>
</div>
`
};

// 2. 调宿申请
const Apply = {
    data() {
        return {
            form: { studentId: "", targetDormId: "", reason: "" },
            myList: [],
            userRole: "" // 🟢 引入角色控制
        }
    },
    mounted() {
        // 🟢 加载时获取角色
        try {
            const u = localStorage.user;
            if (u) this.userRole = JSON.parse(u).role || "USER";
        } catch(e) {
            this.userRole = "USER";
        }

        if (this.userRole === 'USER') {
            this.getMyList();
        }
    },
    methods: {
        async getMyList() {
            const res = await axios.get("/exchange/list");
            const user = JSON.parse(localStorage.user || "{}");
            this.myList = (res.data.data || []).filter(item => item.studentId === user.username);
        },
        async submit() {
            try {
                const user = JSON.parse(localStorage.user || "{}");
                if (!user.username) {
                    this.$message.error("未登录或登录失效！");
                    return;
                }
                this.form.studentId = user.username;
                await axios.post("/exchange/apply", this.form);
                this.$message.success("申请提交成功");
                this.form = { studentId: "", targetDormId: "", reason: "" };
                this.getMyList();
            } catch (e) {
                this.$message.error("提交失败");
            }
        }
    },
    template: `
<div>
    <div v-if="userRole === 'ADMIN'" style="padding: 20px; color: #909399; text-align: center;">
        <el-alert title="管理提示" type="warning" description="您当前是系统管理员身份，调宿申请功能仅对普通学生用户开放。" show-icon :closable="false"></el-alert>
    </div>

    <div v-else>
        <h3>调宿申请</h3>
        <el-form :model="form" label-width="120px">
            <el-form-item label="目标宿舍号">
                <el-input v-model="form.targetDormId"></el-input>
            </el-form-item>
            <el-form-item label="申请理由">
                <el-input type="textarea" v-model="form.reason"></el-input>
            </el-form-item>
            <el-form-item>
                <el-button type="primary" @click="submit">提交申请</el-button>
            </el-form-item>
        </el-form>

        <h4 style="margin-top:20px">我的申请记录</h4>
        <el-table :data="myList" border style="margin-top:10px">
            <el-table-column prop="currentDormId" label="当前宿舍"></el-table-column>
            <el-table-column prop="targetDormId" label="目标宿舍"></el-table-column>
            <el-table-column prop="reason" label="理由"></el-table-column>
            <el-table-column prop="status" label="状态"></el-table-column>
        </el-table>
    </div>
</div>
`
};

// 3. 调宿审批（管理员）
const Audit = {
    data() {
        return {
            allList: [],
            userRole: "" // 🟢 引入角色控制
        }
    },
    mounted() {
        try {
            const u = localStorage.user;
            if (u) this.userRole = JSON.parse(u).role || "USER";
        } catch(e) {
            this.userRole = "USER";
        }

        if (this.userRole === 'ADMIN') {
            this.getAllList();
        }
    },
    methods: {
        async getAllList() {
            const res = await axios.get("/exchange/list");
            this.allList = res.data.data || [];
        },
        async audit(id, status) {
            try {
                await axios.put("/exchange/audit", { id, status });
                this.$message.success("操作成功");
                this.getAllList();
            } catch (e) {
                this.$message.error("操作失败");
            }
        }
    },
    template: `
<div>
    <div v-if="userRole !== 'ADMIN'" style="padding: 20px;">
        <el-alert title="无权查看" type="error" description="对不起，您不是系统管理员，无权进入审批中心！" show-icon :closable="false"></el-alert>
    </div>

    <div v-else>
        <h3>调宿申请审批（管理员）</h3>
        <el-table :data="allList" border style="margin-top:10px">
            <el-table-column prop="studentId" label="学号"></el-table-column>
            <el-table-column prop="currentDormId" label="当前宿舍"></el-table-column>
            <el-table-column prop="targetDormId" label="目标宿舍"></el-table-column>
            <el-table-column prop="reason" label="理由"></el-table-column>
            <el-table-column prop="status" label="状态"></el-table-column>
            <el-table-column label="操作">
                <template slot-scope="scope">
                    <el-button v-if="scope.row.status === 'PENDING'" type="success" size="mini" @click="audit(scope.row.id, 'APPROVED')">同意</el-button>
                    <el-button v-if="scope.row.status === 'PENDING'" type="danger" size="mini" @click="audit(scope.row.id, 'REJECTED')">拒绝</el-button>
                    <span v-else>已处理</span>
                </template>
            </el-table-column>
        </el-table>
    </div>
</div>
`
};

// 4. 登录页
const Login = {
    data() {
        return {
            form: { username: "", password: "" }
        }
    },
    methods: {
        // 登录 POST
        async login() {
            try {
                const res = await axios.post("/user/login", this.form);
                if (res.data.code === 200) {
                    localStorage.user = JSON.stringify(res.data.data);
                    this.$root.user = res.data.data; // 👈 加上这行！让登录成功后 index.html 的菜单能立刻亮起来！
                    this.$router.push("/dorm");
                } else {
                    this.$message.error(res.data.msg);
                }
            } catch (e) {
                this.$message.error("登录失败");
            }
        },
        async register() {
            try {
                const res = await axios.post("/user/register", this.form);
                this.$message.success(res.data.msg);
            } catch (e) {
                this.$message.error("注册失败");
            }
        }
    },
    template: `
<div style="width:420px;margin:120px auto">
    <el-card header="宿舍管理系统登录">
        <el-form :model="form">
            <el-form-item label="用户名">
                <el-input v-model="form.username"></el-input>
            </el-form-item>
            <el-form-item label="密码">
                <el-input type="password" v-model="form.password"></el-input>
            </el-form-item>
            <el-form-item>
                <el-button type="primary" @click="login">登录</el-button>
                <el-button @click="register">注册</el-button>
            </el-form-item>
        </el-form>
    </el-card>
</div>
`
};

const routes = [
    { path: "/", redirect: "/dorm" },
    { path: "/dorm", component: Dorm },
    { path: "/apply", component: Apply },
    { path: "/audit", component: Audit },
    { path: "/login", component: Login }
];

const router = new VueRouter({ routes });

router.beforeEach((to, from, next) => {
    const user = localStorage.user;
    if (!user && to.path !== "/login") next("/login");
    else next();
});

new Vue({
    router,
    data() {
        return {
            user: {}
        }
    },
    created() {
        const u = localStorage.user;
        if (u) this.user = JSON.parse(u);
    },
    methods: {
        logout() {
            localStorage.clear();
            this.$router.push("/login");
            this.user = {};
        }
    }
}).$mount("#app");