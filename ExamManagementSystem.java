import java.awt.*;
import java.awt.event.*;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ExamManagementSystem extends Frame implements ActionListener {
    private static final String DB_URL = "jdbc:sqlite:exam_management.db";
    private Connection conn;
    private final Color navy = new Color(15, 45, 80);
    private final Color blue = new Color(30, 100, 165);
    private final Color lightBlue = new Color(232, 243, 252);
    private final Color white = Color.WHITE;
    private final Color darkText = new Color(35, 48, 65);
    private final Color red = new Color(185, 55, 55);
    private final Font collegeFont = new Font("Arial", Font.BOLD, 22);
    private final Font titleFont = new Font("Arial", Font.BOLD, 18);
    private final Font headingFont = new Font("Arial", Font.BOLD, 15);
    private final Font normalFont = new Font("Arial", Font.PLAIN, 13);
    private final Font buttonFont = new Font("Arial", Font.BOLD, 11);
    private final CardLayout card = new CardLayout();
    private final Panel mainPanel = new Panel(card);
    private final TextField tfUsername = new TextField();
    private final TextField tfPassword = new TextField();
    private final Label lblLoginMessage = new Label(" ", Label.CENTER);
    private final Button btnAdminLogin = new Button("ADMIN LOGIN");
    private final Button btnStudentLogin = new Button("STUDENT LOGIN");
    private final Button btnLogin = new Button("LOGIN");
    private final Button btnLoginBack = new Button("BACK");
    private String selectedRole = "";
    private String loggedInStudentId = "";
    private final TextField tfId = new TextField();
    private final TextField tfName = new TextField();
    private final TextField tfDepartment = new TextField();
    private final TextField tfYear = new TextField();
    private final TextField tfSubject = new TextField();
    private final TextField tfMarks = new TextField();
    private final TextArea taAdminDisplay = new TextArea();
    private final Button btnAdd = new Button("ADD STUDENT");
    private final Button btnUpdate = new Button("EDIT RECORD");
    private final Button btnDelete = new Button("DELETE");
    private final Button btnView = new Button("VIEW ALL");
    private final Button btnClear = new Button("CLEAR");
    private final Button btnLogout = new Button("LOGOUT");
    private final Label lblStudentWelcome = new Label("", Label.CENTER);
    private final Label lblStudentInstruction = new Label("", Label.CENTER);
    private final TextArea taStudentDisplay = new TextArea();
    private final Button btnMyResults = new Button("VIEW MY RESULTS");
    private final Button btnStudentBack = new Button("LOGOUT");
    private static class StudentRecord {
        String name;
        String department;
        String year;
        LinkedHashMap<String, Integer> subjects = new LinkedHashMap<>();
        StudentRecord(String name, String department, String year) {
            this.name = name;
            this.department = department;
            this.year = year;
        }
    }
    public ExamManagementSystem() {
        super("K. Ramakrishnan College of Technology - Exam Portal");
        setLayout(new BorderLayout());
        setSize(1100, 720);
        setMinimumSize(new Dimension(800, 550));
        setLocationRelativeTo(null);
        setBackground(lightBlue);
        initDatabase();
        createHomePage();
        createLoginPage();
        createAdminPage();
        createStudentPage();
        add(mainPanel, BorderLayout.CENTER);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeDatabase();
                dispose();
                System.exit(0);
            }
        });
        card.show(mainPanel, "home");
        setVisible(true);
    }
    private void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(DB_URL);
            try (Statement st = conn.createStatement()) {
                // Needed so that deleting a student also deletes his/her marks
                st.execute("PRAGMA foreign_keys = ON");
                st.execute(
                    "CREATE TABLE IF NOT EXISTS students (" +
                    "  student_id  TEXT PRIMARY KEY," +
                    "  name        TEXT NOT NULL," +
                    "  department  TEXT NOT NULL," +
                    "  year        TEXT NOT NULL" +
                    ")"
                );
                st.execute(
                    "CREATE TABLE IF NOT EXISTS marks (" +
                    "  student_id  TEXT NOT NULL," +
                    "  subject     TEXT NOT NULL," +
                    "  marks       INTEGER NOT NULL CHECK (marks BETWEEN 0 AND 100)," +
                    "  PRIMARY KEY (student_id, subject)," +
                    "  FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE" +
                    ")"
                );
            }
            loadSampleDataIfEmpty();

        } catch (ClassNotFoundException e) {
            fatalDbError("SQLite JDBC driver not found.\nAdd sqlite-jdbc jar to the classpath.");
        } catch (SQLException e) {
            fatalDbError("Could not open database:\n" + e.getMessage());
        }
    }

    private void fatalDbError(String message) {
        System.err.println(message);
        System.exit(1);
    }

    private void closeDatabase() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException ignored) {
        }
    }

    /** Inserts the two demo students only when the table is empty (first run). */
    private void loadSampleDataIfEmpty() throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM students")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }
        StudentRecord s1 = new StudentRecord("Arun Kumar", "Computer Science", "3rd Year");
        s1.subjects.put("Java Programming", 88);
        s1.subjects.put("Database Systems", 92);
        s1.subjects.put("Operating Systems", 79);
        saveRecord("S101", s1);

        StudentRecord s2 = new StudentRecord("Priya Sharma", "Electronics", "2nd Year");
        s2.subjects.put("Digital Logic", 95);
        s2.subjects.put("Circuit Theory", 84);
        saveRecord("S102", s2);
    }
    private boolean studentExists(String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM students WHERE student_id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
    private StudentRecord getStudent(String id) throws SQLException {
        StudentRecord student = null;

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT name, department, year FROM students WHERE student_id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    student = new StudentRecord(rs.getString("name"),
                                                rs.getString("department"),
                                                rs.getString("year"));
                }
            }
        }

        if (student == null) {
            return null;
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT subject, marks FROM marks WHERE student_id = ? ORDER BY rowid")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    student.subjects.put(rs.getString("subject"), rs.getInt("marks"));
                }
            }
        }
        return student;
    }
    private LinkedHashMap<String, StudentRecord> getAllStudents() throws SQLException {
        LinkedHashMap<String, StudentRecord> all = new LinkedHashMap<>();

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT student_id FROM students ORDER BY student_id")) {
            while (rs.next()) {
                all.put(rs.getString(1), null);
            }
        }

        for (String id : new java.util.ArrayList<>(all.keySet())) {
            all.put(id, getStudent(id));
        }
        return all;
    }
    private void saveRecord(String id, StudentRecord student) throws SQLException {
        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO students (student_id, name, department, year) VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT(student_id) DO UPDATE SET " +
                    "name = excluded.name, department = excluded.department, year = excluded.year")) {
                ps.setString(1, id);
                ps.setString(2, student.name);
                ps.setString(3, student.department);
                ps.setString(4, student.year);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM marks WHERE student_id = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO marks (student_id, subject, marks) VALUES (?, ?, ?)")) {
                for (Map.Entry<String, Integer> entry : student.subjects.entrySet()) {
                    ps.setString(1, id);
                    ps.setString(2, entry.getKey());
                    ps.setInt(3, entry.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }
    private void deleteStudent(String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM students WHERE student_id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }
    private void styleButton(Button b) {
        b.setFont(buttonFont);
        b.setBackground(blue);
        b.setForeground(white);
        b.setPreferredSize(new Dimension(115, 28));
    }
    private Label makeLabel(String text) {
        Label label = new Label(text);
        label.setFont(normalFont);
        label.setForeground(darkText);
        return label;
    }
    private void styleField(TextField field) {
        field.setFont(normalFont);
        field.setBackground(white);
        field.setForeground(darkText);
        field.setPreferredSize(new Dimension(210, 26));
    }
    private void styleDisplay(TextArea area) {
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setBackground(white);
        area.setForeground(darkText);
    }
    private Panel createHeader(String pageTitle) {
        Panel header = new Panel(new BorderLayout(10, 5));
        header.setBackground(navy);
        Canvas logo = new Canvas() {
            private Image image;
            {
                try {
                    image = ImageIO.read(new File("A:/JAVAPROJECTKRCT/college_logo.png"));
                } catch (IOException ex) {
                    // Fallback handled in paint
                }
            }
            @Override
            public void paint(Graphics g) {
                if (image == null) {
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Arial", Font.BOLD, 10));
                    g.drawString("KRCT", 15, 45);
                    return;
                }
                int iw = image.getWidth(null);
                int ih = image.getHeight(null);
                if (iw <= 0 || ih <= 0) return;
                int boxW = getWidth() - 12;
                int boxH = getHeight() - 10;
                double scale = Math.min((double) boxW / iw, (double) boxH / ih);
                int w = (int) (iw * scale);
                int h = (int) (ih * scale);
                g.drawImage(image, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
            }
        };
        logo.setBackground(navy);
        logo.setPreferredSize(new Dimension(100, 85));
        Panel titles = new Panel(new GridLayout(2, 1, 2, 2));
        titles.setBackground(navy);
        Label college = new Label("K. RAMAKRISHNAN COLLEGE OF TECHNOLOGY", Label.CENTER);
        college.setFont(collegeFont);
        college.setForeground(white);
        Label title = new Label(pageTitle, Label.CENTER);
        title.setFont(titleFont);
        title.setForeground(new Color(175, 215, 255));
        titles.add(college);
        titles.add(title)
        header.add(logo, BorderLayout.WEST);
        header.add(titles, BorderLayout.CENTER);
        header.setPreferredSize(new Dimension(100, 95));
        return header;
    }
    private Panel createPage(String pageTitle) {
        Panel page = new Panel(new BorderLayout(10, 10));
        page.setBackground(lightBlue);
        page.add(createHeader(pageTitle), BorderLayout.NORTH);
        return page;
    }
    private Panel createButtonPanel(Button... buttons) {
        Panel panel = new Panel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        panel.setBackground(lightBlue);
        for (Button b : buttons) {
            styleButton(b);
            b.addActionListener(this);
            panel.add(b);
        }
        return panel;
    }
    private Panel createSectionTitle(String text) {
        Panel panel = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panel.setBackground(lightBlue);
        Label title = new Label(text);
        title.setFont(headingFont);
        title.setForeground(navy);
        panel.add(title);
        return panel;
    }
    private void createHomePage() {
        Panel page = createPage("EXAMINATION & STUDENT MANAGEMENT PORTAL");
        Panel content = new Panel(new GridBagLayout());
        content.setBackground(lightBlue);
        Panel box = new Panel(new GridLayout(5, 1, 8, 10));
        box.setBackground(white);
        Label welcome = new Label("WELCOME TO THE EXAMINATION PORTAL", Label.CENTER);
        welcome.setFont(new Font("Arial", Font.BOLD, 19));
        welcome.setForeground(navy);
        Label description = new Label("Manage examinations, student records and semester results securely", Label.CENTER);
        description.setFont(normalFont);
        description.setForeground(darkText);
        Label instruction = new Label("Please select your portal login type to proceed", Label.CENTER);
        instruction.setFont(headingFont);
        instruction.setForeground(blue);
        styleButton(btnAdminLogin);
        styleButton(btnStudentLogin);
        btnAdminLogin.setPreferredSize(new Dimension(160, 32));
        btnStudentLogin.setPreferredSize(new Dimension(160, 32))
        btnAdminLogin.addActionListener(this);
        btnStudentLogin.addActionListener(this);
        Panel adminPanelBtn = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        adminPanelBtn.setBackground(white);
        adminPanelBtn.add(btnAdminLogin);
        Panel studentPanelBtn = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        studentPanelBtn.setBackground(white);
        studentPanelBtn.add(btnStudentLogin);
        box.add(welcome);
        box.add(description);
        box.add(instruction);
        box.add(adminPanelBtn);
        box.add(studentPanelBtn);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(25, 50, 25, 50);
        content.add(box, gbc);
        page.add(content, BorderLayout.CENTER);
        mainPanel.add(page, "home");
    }
    private void createLoginPage() {
        Panel page = createPage("SECURE PORTAL LOGIN");
        Panel wrapper = new Panel(new GridBagLayout());
        wrapper.setBackground(lightBlue)
        Panel form = new Panel(new GridLayout(3, 2, 8, 8));
        form.setBackground(lightBlue)
        tfUsername.setFont(normalFont);
        tfUsername.setBackground(white);
        tfUsername.setForeground(darkText);
        tfUsername.setPreferredSize(new Dimension(150, 24))
        tfPassword.setFont(normalFont);
        tfPassword.setBackground(white);
        tfPassword.setForeground(darkText);
        tfPassword.setPreferredSize(new Dimension(150, 24));
        tfPassword.setEchoChar('*')
        btnLogin.setFont(buttonFont);
        btnLogin.setBackground(blue);
        btnLogin.setForeground(white);
        btnLogin.setPreferredSize(new Dimension(90, 26));
        btnLogin.addActionListener(this);
        btnLoginBack.setFont(buttonFont);
        btnLoginBack.setBackground(blue);
        btnLoginBack.setForeground(white);
        btnLoginBack.setPreferredSize(new Dimension(90, 26));
        btnLoginBack.addActionListener(this);
        form.add(makeLabel("USER ID / USERNAME"));
        form.add(tfUsername);
        form.add(makeLabel("PASSWORD"));
        form.add(tfPassword);
        form.add(btnLoginBack);
        form.add(btnLogin)
        lblLoginMessage.setFont(normalFont);
        lblLoginMessage.setForeground(red);
        Panel content = new Panel(new BorderLayout(8, 10));
        content.setBackground(lightBlue);
        content.add(createSectionTitle("Enter your portal credentials"), BorderLayout.NORTH);
        content.add(form, BorderLayout.CENTER);
        content.add(lblLoginMessage, BorderLayout.SOUTH);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(20, 40, 20, 40);
        wrapper.add(content, gbc);
        page.add(wrapper, BorderLayout.CENTER);
        mainPanel.add(page, "login");
    }
    private void openLogin(String role) {
        selectedRole = role;
        tfUsername.setText("");
        tfPassword.setText("");
        lblLoginMessage.setText(" ");

        Panel loginPage = (Panel) mainPanel.getComponent(1);
        Panel header = (Panel) loginPage.getComponent(0);
        Panel titles = (Panel) header.getComponent(1);
        Label title = (Label) titles.getComponent(1);

        title.setText(role.toUpperCase() + " AUTHENTICATION PORTAL");
        card.show(mainPanel, "login");
        tfUsername.requestFocus();
    }
    private void createAdminPage() {
        Panel page = createPage("ADMINISTRATION & EXAM RECORDS");
        Panel content = new Panel(new BorderLayout(6, 6));
        content.setBackground(lightBlue)
        Panel formSection = new Panel(new BorderLayout(4, 4));
        formSection.setBackground(lightBlue)
        formSection.add(createSectionTitle("STUDENT INFORMATION ENTRY & MODIFICATION"), BorderLayout.NORTH)
        Panel form = new Panel(new GridBagLayout());
        form.setBackground(lightBlue)
        styleField(tfId);
        styleField(tfName);
        styleField(tfDepartment);
        styleField(tfYear);
        styleField(tfSubject);
        styleField(tfMarks);
        tfSubject.setPreferredSize(new Dimension(210, 26));
        tfMarks.setPreferredSize(new Dimension(210, 26));
        addFormRow(form, 0, "Student ID", tfId);
        addFormRow(form, 1, "Student Name", tfName);
        addFormRow(form, 2, "Department", tfDepartment);
        addFormRow(form, 3, "Year / Semester", tfYear);
        addFormRow(form, 4, "Subjects", tfSubject);
        addFormRow(form, 5, "Marks", tfMarks);
        Panel formWrapper = new Panel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        formWrapper.setBackground(lightBlue);
        formWrapper.add(form);
        formSection.add(formWrapper, BorderLayout.CENTER);
        Panel actions = createButtonPanel(
            btnAdd, btnUpdate, btnDelete, btnView, btnClear, btnLogout
        );
        styleDisplay(taAdminDisplay);
        Panel resultSection = new Panel(new BorderLayout(4, 4));
        resultSection.setBackground(lightBlue)
        resultSection.add(createSectionTitle("SYSTEM LOGS & STUDENT RECORDS DATABASE"), BorderLayout.NORTH);
        resultSection.add(taAdminDisplay, BorderLayout.CENTER)
        content.add(formSection, BorderLayout.NORTH);
        content.add(resultSection, BorderLayout.CENTER);
        content.add(actions, BorderLayout.SOUTH);
        page.add(content, BorderLayout.CENTER);
        mainPanel.add(page, "admin");
    }
    private void addFormRow(Panel form, int row, String label, TextField field) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = row;
        gbc.insets = new Insets(3, 8, 3, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        form.add(makeLabel(label + ":"), gbc)
        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        form.add(field, gbc);
    }
    private void createStudentPage() {
        Panel page = createPage("STUDENT RESULT DASHBOARD");
        lblStudentWelcome.setFont(new Font("Arial", Font.BOLD, 20));
        lblStudentWelcome.setForeground(navy);
        lblStudentInstruction.setFont(new Font("Arial", Font.BOLD, 13));
        lblStudentInstruction.setForeground(blue)
        Panel info = new Panel(new GridLayout(2, 1, 4, 4));
        info.setBackground(lightBlue);
        info.add(lblStudentWelcome);
        info.add(lblStudentInstruction);
        styleDisplay(taStudentDisplay);
        taStudentDisplay.setFont(new Font("Monospaced", Font.PLAIN, 13));
        Panel results = new Panel(new BorderLayout(6, 6));
        results.setBackground(lightBlue);

        results.add(createSectionTitle("SEMESTER EXAMINATION RESULTS & MARKSHEET"), BorderLayout.NORTH);
        results.add(taStudentDisplay, BorderLayout.CENTER);
        Panel center = new Panel(new BorderLayout(8, 8));
        center.setBackground(lightBlue);
        center.add(info, BorderLayout.NORTH);
        center.add(results, BorderLayout.CENTER);
        center.add(createButtonPanel(btnMyResults, btnStudentBack), BorderLayout.SOUTH);
        page.add(center, BorderLayout.CENTER);
        mainPanel.add(page, "student");
    }
@Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == btnAdminLogin) {
            openLogin("Admin");
            return;
        }
        if (source == btnStudentLogin) {
            openLogin("Student");
            return;
        }
        if (source == btnLoginBack) {
            card.show(mainPanel, "home");
            return;
        }
        try {
            if (source == btnLogin) {
                handleLogin();
                return;
            }
            if (source == btnLogout || source == btnStudentBack) {
                loggedInStudentId = "";
                tfUsername.setText("");
                tfPassword.setText("");
                lblLoginMessage.setText(" ");
                card.show(mainPanel, "home");
                return;
            }
            if (source == btnClear) {
                clearFields();
                taAdminDisplay.setText("Input fields cleared successfully.");
                return;
            }
            if (source == btnMyResults) {
                displayStudentResults();
                return;
            }
            String id = tfId.getText().trim();
            String name = tfName.getText().trim();
            String department = tfDepartment.getText().trim();
            String year = tfYear.getText().trim();
            String subjects = tfSubject.getText().trim();
            String marks = tfMarks.getText().trim();
            if (source == btnAdd) {
                if (id.isEmpty() || subjects.isEmpty() || marks.isEmpty()) {
                    taAdminDisplay.setText("Error: Student ID, Subjects and Marks are required.");
                    return;
                }
                if (studentExists(id)) {
                    addSubjectsToExisting(id, subjects, marks);
                    return;
                }
                if (name.isEmpty() || department.isEmpty() || year.isEmpty()) {
                    taAdminDisplay.setText("Error: New Student ID. Please fill Name, Department and Year as well.");
                    return;
                }
                saveNewRecord(id, name, department, year, subjects, marks);
                return;
            }
            if (source == btnUpdate) {
                if (id.isEmpty()) {
                    taAdminDisplay.setText("Error: Enter the Student ID of the record you wish to edit.");
                    return;
                }
                if (!studentExists(id)) {
                    taAdminDisplay.setText("Error: Student ID not found in database.");
                    return;
                }
                updateRecord(id, name, department, year, subjects, marks);
                return;

            if (source == btnDelete) {
                if (id.isEmpty() || !studentExists(id)) {
                    taAdminDisplay.setText("Error: Enter a valid existing Student ID to delete.");
                    return;
                }
                deleteStudent(id);
                taAdminDisplay.setText("Student record deleted successfully.\nDeleted Student ID: " + id);
                clearFields();
                return;
            }
            if (source == btnView) {
                displayAllRecords();

        } catch (SQLException ex) {
            String msg = "Database Error: " + ex.getMessage();
            taAdminDisplay.setText(msg);
            taStudentDisplay.setText(msg);
            lblLoginMessage.setText("Database Error. Please try again.");
        }
    }
    private void handleLogin() throws SQLException {
        String username = tfUsername.getText().trim();
        String password = tfPassword.getText().trim();

        if (selectedRole.equals("Admin") && username.equals("admin") && password.equals("admin123")) {
            card.show(mainPanel, "admin");
            taAdminDisplay.setText(
                "Welcome, Administrator!\n\n" +
                "Student records stored in database: " + getAllStudents().keySet() + "\n" +
                "Use the form above to add new student records or enter a Student ID to edit/delete."
            );
        } else if (selectedRole.equals("Student") && !username.isEmpty() && password.equals("student123")) {
            loggedInStudentId = username;
            StudentRecord student = getStudent(username);
            String studentName = student == null ? "Student" : student.name;

            lblStudentWelcome.setText("Welcome, " + studentName + "!");
            lblStudentInstruction.setText("Student ID: " + username + "   |   Click VIEW MY RESULTS below to check your grades.");
            taStudentDisplay.setText(
                "Student Portal Session Initialized.\n\n" +
                "Click 'VIEW MY RESULTS' to load your verified semester mark sheet."
            );
            card.show(mainPanel, "student");
        } else {
            lblLoginMessage.setText("Invalid credentials. Admin: admin/admin123 | Student: S101 or S102 / student123");
        }
    }
    private String[] splitValues(String text) {
        return text.split("[,\\n\\r]+");
    }
    private boolean fillSubjects(StudentRecord student, String subjectText, String markText, boolean replaceExisting) {
        String[] subjects = splitValues(subjectText);
        String[] marks = splitValues(markText);
        if (subjects.length != marks.length) {
            taAdminDisplay.setText("Error: The count of subjects and marks must match.");
            return false;

        for (int i = 0; i < subjects.length; i++) {
            String subject = subjects[i].trim();
            String markString = marks[i].trim();
            if (subject.isEmpty() || markString.isEmpty()) {
                taAdminDisplay.setText("Error: Subject names and marks cannot be empty.");
                return false;
            }

            int value;
            try {
                value = Integer.parseInt(markString);
            } catch (NumberFormatException ex) {
                taAdminDisplay.setText("Error: Marks must be valid whole numbers.");
                return false;
            }
            if (value < 0 || value > 100) {
                taAdminDisplay.setText("Error: Marks must be within the range of 0 to 100.");
                return false;
            }
            if (replaceExisting || !student.subjects.containsKey(subject)) {
                student.subjects.put(subject, value);
            } else {
                taAdminDisplay.setText("Duplicate subject detected: " + subject +
                    "\nThis subject already exists for this student. Use EDIT RECORD to change its marks.");
                return false;
            }
        }
        return true;
    }
    private void saveNewRecord(String id, String name, String department, String year, String subjects, String marks) throws SQLException {
        StudentRecord student = new StudentRecord(name, department, year);
        if (!fillSubjects(student, subjects, marks, false)) {
            return;
        }
        saveRecord(id, student);
        taAdminDisplay.setText("STUDENT RECORD ADDED SUCCESSFULLY\n\n" + formatRecord(id, student));
        clearFields();
    }

    private void addSubjectsToExisting(String id, String subjects, String marks) throws SQLException {
        StudentRecord existing = getStudent(id);
        if (!fillSubjects(existing, subjects, marks, false)) {
            return;
        }
        saveRecord(id, existing);
        taAdminDisplay.setText("NEW SUBJECTS ADDED TO EXISTING STUDENT\n\n" + formatRecord(id, existing));
        clearFields();
    }
    private void updateRecord(String id, String name, String department, String year, String subjects, String marks) throws SQLException {
        StudentRecord old = getStudent(id);
        StudentRecord updated = new StudentRecord(old.name, old.department, old.year);
        updated.subjects.putAll(old.subjects);
        if (!name.isEmpty()) updated.name = name;
        if (!department.isEmpty()) updated.department = department;
        if (!year.isEmpty()) updated.year = year;
        if (!subjects.isEmpty() && !fillSubjects(updated, subjects, marks, true)) {
            return;
        }
        saveRecord(id, updated);
        taAdminDisplay.setText("STUDENT RECORD UPDATED SUCCESSFULLY\n\n" + formatRecord(id, updated));
        clearFields();
    }
    private String formatRecord(String id, StudentRecord student) {
        StringBuilder sb = new StringBuilder();
        int total = 0;
        boolean pass = true;
        sb.append("Student ID   : ").append(id).append("\n");
        sb.append("Student Name : ").append(student.name).append("\n");
        sb.append("Department   : ").append(student.department).append("\n");
        sb.append("Year         : ").append(student.year).append("\n\n");

        sb.append(String.format("%-28s %s%n", "SUBJECT NAME", "MARKS"));
        sb.append("--------------------------------------------------\n");
        for (Map.Entry<String, Integer> entry : student.subjects.entrySet()) {
            sb.append(String.format("%-28s %d%n", entry.getKey(), entry.getValue()));
            total += entry.getValue();
            if (entry.getValue() < 40) {
                pass = false;
            }
        }
        int count = student.subjects.size();
        double percentage = count == 0 ? 0 : (double) total / count;
        sb.append("--------------------------------------------------\n");
        sb.append("Total Marks  : ").append(total).append("/").append(count * 100).append("\n");
        sb.append(String.format("Percentage   : %.2f%%%n", percentage));
        sb.append("Result Status: ").append(pass ? "PASS" : "FAIL");
        return sb.toString();
    }
    private void displayAllRecords() throws SQLException {
        LinkedHashMap<String, StudentRecord> records = getAllStudents();
        if (records.isEmpty()) {
            taAdminDisplay.setText("No student records found in database.");
            return;
        }
    }
    private void displayStudentResults() throws SQLException {
        StudentRecord student = getStudent(loggedInStudentId);
        if (student == null) {
            taStudentDisplay.setText("No examination record found for Student ID: " + loggedInStudentId);
            return;
        }

        taStudentDisplay.setText(
            "             OFFICIAL SEMESTER EXAMINATION RESULTS\n" +
            "             K. RAMAKRISHNAN COLLEGE OF TECHNOLOGY\n\n" +
            formatRecord(loggedInStudentId, student)
        );
    }
    private void clearFields() {
        tfId.setText("");
        tfName.setText("");
        tfDepartment.setText("");
        tfYear.setText("");
        tfSubject.setText("");
        tfMarks.setText("");
    }
    public static void main(String[] args) {
        EventQueue.invokeLater(ExamManagementSystem::new);
    }
}
