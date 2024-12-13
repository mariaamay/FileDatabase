import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

public class FileDatabaseGUI extends JFrame {
    private JTable table;
    private DefaultTableModel tableModel;
    private final String fileName;
    private final String idFileName;
    private final String directory;
    private final HashMap<Integer, Long> map = new HashMap<>();

    public FileDatabaseGUI(String fileName, String idFileName, String directory) {
        this.directory = directory;
        this.fileName = fileName;
        this.idFileName = idFileName;

        setTitle("Управление базой данных");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        JPanel panelCenter = new JPanel();
        JPanel panelUp = new JPanel();
        panel.setLayout(new GridLayout(2, 1));
        panelUp.setLayout(new GridLayout(2, 1));

        tableModel = new DefaultTableModel();
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        panelCenter.add(scrollPane);

        JTextField recordField = new JTextField(30);
        panelCenter.add(recordField);

        JButton createDbButton = new JButton("Создать БД");
        createDbButton.setFocusable(false);

        createDbButton.addActionListener(e -> createDatabase());
        panel.add(createDbButton);

        JButton deleteDbButton = new JButton("Удалить БД");
        deleteDbButton.setFocusable(false);
        deleteDbButton.addActionListener(e -> deleteDatabase());
        panel.add(deleteDbButton);

        JButton clearDbButton = new JButton("Очистить БД");
        clearDbButton.setFocusable(false);
        clearDbButton.addActionListener(e -> clearDatabase());
        panel.add(clearDbButton);

        JButton openDbButton = new JButton("Открыть БД");
        openDbButton.setFocusable(false);
        openDbButton.addActionListener(e -> openDatabaseFromUserInput());
        panel.add(openDbButton);

        JButton saveDbButton = new JButton("Сохранить БД");
        saveDbButton.setFocusable(false);
        saveDbButton.addActionListener(e -> saveDatabaseFromUserInput());
        panel.add(saveDbButton);

        JButton addButton = new JButton("Добавить новую запись");
        addButton.setFocusable(false);
        addButton.addActionListener(e -> addRecord());
        panelUp.add(addButton);

        JButton deleteIdButton = new JButton("Удалить запись по ключевому полю");
        deleteIdButton.setFocusable(false);
        deleteIdButton.addActionListener(e -> deleteRecordById());
        panelUp.add(deleteIdButton);

        JButton deleteFieldButton = new JButton("Удалить запись по неключевому полю");
        deleteFieldButton.setFocusable(false);
        deleteFieldButton.addActionListener(e -> deleteRecordByField());
        panelUp.add(deleteFieldButton);

        JButton searchByIdButton = new JButton("Найти запись по ключевому полю");
        searchByIdButton.setFocusable(false);
        searchByIdButton.addActionListener(e -> searchById());
        panelUp.add(searchByIdButton);

        JButton searchByFieldButton = new JButton("Найти запись по неключевому полю");
        searchByFieldButton.setFocusable(false);
        searchByFieldButton.addActionListener(e -> searchByField());
        panelUp.add(searchByFieldButton);

        JButton editButton = new JButton("Редактировать запись");
        editButton.setFocusable(false);
        editButton.addActionListener(e -> editRecord());
        panelUp.add(editButton);

        JButton createBackupButton = new JButton("Создать бэкап");
        createBackupButton.setFocusable(false);
        createBackupButton.addActionListener(e -> createBackup());
        panel.add(createBackupButton);

        JButton loadBackupButton = new JButton("Восстановить БД");
        loadBackupButton.setFocusable(false);
        loadBackupButton.addActionListener(e -> loadBackup());
        panel.add(loadBackupButton);

        JButton importDbButton = new JButton("Импорт в .xlsx");
        importDbButton.addActionListener(e -> importToExcel());
        importDbButton.setFocusable(false);
        panel.add(importDbButton);

        add(panel, BorderLayout.SOUTH);
        add(panelUp, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

    }

    private void createDatabase() {
        String header = JOptionPane.showInputDialog(this, "Введите заголовок базы данных:", "Создание базы данных", JOptionPane.PLAIN_MESSAGE);
        if (header != null && !header.isEmpty()) {

            // Создаем директорию, если она не существует
            File dbDirectory = new File(directory);
            if (!dbDirectory.exists()) {
                if (dbDirectory.mkdirs()) {
                    JOptionPane.showMessageDialog(this, "Директория '" + directory + "' успешно создана.", "Создание базы данных", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Директория с именем '" + directory + "' уже существует.", "Создание базы данных", JOptionPane.ERROR_MESSAGE);
            }


            // Полные пути к файлам
            File dbFile = new File(dbDirectory, fileName);
            File idFile = new File(dbDirectory, idFileName);

            // Проверяем, существует ли файл базы данных
            if (dbFile.exists()) {
                // Если файл существует, запрашиваем у пользователя, хочет ли он перезаписать его
                int ans = JOptionPane.showConfirmDialog(this, "Файл базы данных уже существует. Перезаписать? ", "Создание базы данных", JOptionPane.YES_NO_OPTION);
                if (ans == JOptionPane.NO_OPTION) {
                    JOptionPane.showMessageDialog(this, "Создание базы данных отменено.", "Создание базы данных", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                // Удаляем существующий файл
                if (!dbFile.delete()) {
                    JOptionPane.showMessageDialog(this, "Ошибка при удалении существующего файла базы данных.", "Создание базы данных", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // Создаем новый файл базы данных и индексный файл
            try {
                // Создание базы данных
                Files.createFile(dbFile.toPath());
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(dbFile))) {
                    // Записываем заголовок в файл
                    writer.write(header);
                    writer.newLine();
                    JOptionPane.showMessageDialog(this, "База данных успешно создана.", "Создание базы данных", JOptionPane.INFORMATION_MESSAGE);
                    String[] columns = header.split(",");
                    tableModel.setColumnIdentifiers(columns);
                    tableModel.setRowCount(0);
                }

            } catch (FileAlreadyExistsException e) {
                JOptionPane.showMessageDialog(this, "Ошибка: Файл уже существует: " + e.getMessage(), "Создание базы данных", JOptionPane.ERROR_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Ошибка при создании базы данных: " + e.getMessage(), "Создание базы данных", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Заголовок не может быть пустым!", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteDatabase() {
        int confirm = JOptionPane.showConfirmDialog(this, "Вы уверены, что хотите удалить базу данных?", "Удаление базы данных", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {

            File dbDirectory = new File(directory);
            File dbFile = new File(dbDirectory, fileName);
            File idFile = new File(dbDirectory, idFileName);

            // Удаление файла с оффсетами
            if (idFile.exists()) {
                if (idFile.delete()) {
                    JOptionPane.showMessageDialog(this, "Файл индексов успешно удален.", "Удаление базы данных", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Ошибка при удалении файла индексов.", "Удаление базы данных", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Файл индексов не найден.", "Удаление базы данных", JOptionPane.WARNING_MESSAGE);
            }

            // Удаление файла базы данных
            if (dbFile.exists()) {
                if (dbFile.delete()) {
                    JOptionPane.showMessageDialog(this, "База данных успешно удалена.", "Удаление базы данных", JOptionPane.INFORMATION_MESSAGE);
                    tableModel.setRowCount(0);
                    tableModel.setColumnIdentifiers(new Object[0]);
                } else {
                    JOptionPane.showMessageDialog(this, "Ошибка при удалении базы данных.", "Удаление базы данных", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "База данных не найдена.", "Удаление базы данных", JOptionPane.WARNING_MESSAGE);
            }

            // Удаление директории, если она пуста
            if (dbDirectory.isDirectory() && dbDirectory.list().length == 0) {
                if (dbDirectory.delete()) {
                    JOptionPane.showMessageDialog(this, "Директория '" + directory + "' успешно удалена.", "Удаление базы данных", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Ошибка при удалении директории '" + directory + "'.", "Удаление базы данных", JOptionPane.ERROR_MESSAGE);
                }
            }

        }
    }

    private void clearDatabase() {
        int confirm = JOptionPane.showConfirmDialog(this, "Вы уверены, что хотите очистить базу данных?", "Чистка базы данных", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            // Очищаем хэш-мапу
            map.clear();

            // Полные пути к файлам базы данных и индексов
            File dbFile = new File(directory, fileName);
            File idFile = new File(directory, idFileName);

            // Читаем заголовок из файла базы данных
            try (BufferedReader reader = new BufferedReader(new FileReader(dbFile))) {
                String header = reader.readLine(); // Читаем заголовок

                // Записываем только заголовок обратно в файл базы данных
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(dbFile))) {
                    writer.write(header); // Записываем только заголовок
                    writer.newLine(); // Добавляем новую строку после заголовка
                    JOptionPane.showMessageDialog(this, "База данных успешно очищена.", "Очистка базы данных", JOptionPane.INFORMATION_MESSAGE);
                    tableModel.setRowCount(0);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Ошибка при записи в базу данных: " + e.getMessage(), "Очистка базы данных", JOptionPane.ERROR_MESSAGE);
                }

                // Очищаем файл айдишников
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(idFile))) {
                    // Ничего не пишем, просто очищаем файл
                    JOptionPane.showMessageDialog(this, "Файл индексов успешно очищен.", "Очистка базы данных", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Ошибка при записи в файл индексов: " + e.getMessage(), "Очистка базы данных", JOptionPane.ERROR_MESSAGE);
                }

            } catch (FileNotFoundException e) {
                JOptionPane.showMessageDialog(this, "Файл базы данных не найден: " + e.getMessage(), "Очистка базы данных", JOptionPane.ERROR_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Ошибка при чтении файла базы данных: " + e.getMessage(), "Очистка базы данных", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public String askUserForFolderPath() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // Разрешаем выбирать только папки
        fileChooser.setDialogTitle("Выберите папку");

        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = fileChooser.getSelectedFile();
            return selectedFolder.getAbsolutePath(); // Возвращаем путь к выбранной папке
        } else {
            JOptionPane.showMessageDialog(null, "Папка не выбрана.", "Информация", JOptionPane.INFORMATION_MESSAGE);
            return null; // Если папка не выбрана, возвращаем null
        }
    }

    public void openDatabaseFromUserInput() {
        String folderPath = askUserForFolderPath();
        if (folderPath != null) {
            openDatabase(folderPath);
        }
    }

    public void saveDatabaseFromUserInput() {
        String folderPath = askUserForFolderPath();
        if (folderPath != null) {
            saveDatabase(folderPath);
        }
    }

    private void openDatabase(String dbDirectoryPath) {
        // Создаем объект File для целевой директории
        File dbDirectory = new File(dbDirectoryPath); // Директория для сохранённой базы данных
        File backupDbFile = new File(dbDirectory, fileName + "_saved.backup"); // Файл бэкапа базы данных
        File backupIdFile = new File(dbDirectory, idFileName + "_saved.backup"); // Файл бэкапа ID
        File databaseFile = new File(directory, fileName); // Основной файл базы данных
        File dbDirectoryOld = new File(directory);

        if (!backupDbFile.exists() || !backupIdFile.exists()) {
            JOptionPane.showMessageDialog(null, "Ошибка: Не найдены файлы бэкапа базы данных или ID.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (!dbDirectoryOld.exists()) {
                if (dbDirectoryOld.mkdirs()) {
                    JOptionPane.showMessageDialog(null, "Директория '" + directory + "' успешно создана.", "Информация", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "Ошибка при создании директории '" + directory + "'.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return; // Выход из метода, если не удалось создать директорию
                }
            }

            // Проверяем существует ли база данных
            if (!databaseFile.exists()) {
                // Create the database file
                if (databaseFile.createNewFile()) {
                    JOptionPane.showMessageDialog(null, "Файл базы данных успешно создан: " + databaseFile.getPath(), "Информация", JOptionPane.INFORMATION_MESSAGE);

                    // Копируем содержимое из сохраненной базы данных в текущую базу данных чтобы можно было с ней работать
                    try (BufferedReader backupDbReader = new BufferedReader(new FileReader(backupDbFile));
                         BufferedWriter databaseWriter = new BufferedWriter(new FileWriter(databaseFile))) {

                        String line;
                        while ((line = backupDbReader.readLine()) != null) {
                            databaseWriter.write(line);
                            databaseWriter.newLine();
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Ошибка: не удалось создать файл базы данных.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // Загружаем айдишники из файла в хэш-мапу
            try (BufferedReader backupIdReader = new BufferedReader(new FileReader(backupIdFile))) {
                String line;
                while ((line = backupIdReader.readLine()) != null) {
                    String[] parts = line.split(",");

                    if (parts.length == 2) {
                        int id = Integer.parseInt(parts[0]);
                        long mappingValue = Long.parseLong(parts[1]);
                        map.put(id, mappingValue);
                    }
                }
            }

            try (BufferedReader backupDbReader = new BufferedReader(new FileReader(backupDbFile))) {
                String header = backupDbReader.readLine();
                if (header == null || header.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Ошибка: База данных пуста.",
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String[] columnNames = header.split(",");
                DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);

                String line;
                while ((line = backupDbReader.readLine()) != null) {
                    String[] rowData = line.split(",");
                    tableModel.addRow(rowData);
                }

                // Отображаем данные в JTable
                JTable table = new JTable(tableModel);
                JScrollPane scrollPane = new JScrollPane(table);
                JFrame frame = new JFrame("Данные из базы данных");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.add(scrollPane);
                frame.setSize(800, 600);
                frame.setVisible(true);

            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Ошибка при загрузке базы данных", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveDatabase(String targetDirectoryPath) {
        File targetDirectory = new File(targetDirectoryPath); // Директория куда хотим сохранить базу данных
        File dbDirectory = new File(targetDirectory, directory + "_saved");
        File backupDbFile = new File(dbDirectory, fileName + "_saved.backup");
        File backupIdFile = new File(dbDirectory, idFileName + "_saved.backup");

        rewriteTo(targetDirectory, dbDirectory, backupDbFile, backupIdFile);
    }


    private void rewriteTo(File targetDirectory, File dbDirectory, File backupDbFile, File backupIdFile) {
        try {
            // Создаем корневую директорию для бэкапов, если она не существует
            if (!targetDirectory.exists()) {
                if (targetDirectory.mkdir()) {
                    JOptionPane.showMessageDialog(null, "Директория успешно создана: " + targetDirectory.getPath(),
                            "Создание директории", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "Ошибка: не удалось создать директорию.",
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // Создаем директорию db_backup, если она не существует
            if (!dbDirectory.exists()) {
                if (dbDirectory.mkdir()) {
                    JOptionPane.showMessageDialog(null, "Директория для бэкапов базы данных успешно создана: " + dbDirectory.getPath(),
                            "Создание директории", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "Ошибка: не удалось создать директорию для бэкапов базы данных.",
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // Проверяем существование файлов бэкапов и создаем их, если они не существуют
            if (!backupDbFile.exists() && !backupDbFile.createNewFile()) {
                JOptionPane.showMessageDialog(null, "Ошибка: не удалось создать файл бэкапа базы данных.",
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!backupIdFile.exists() && !backupIdFile.createNewFile()) {
                JOptionPane.showMessageDialog(null, "Ошибка: не удалось создать файл бэкапа индексов.",
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }


            int ans = JOptionPane.showConfirmDialog(null, "База данных с таким именем уже существует. Перезаписать? ", "База данных уже существует", JOptionPane.YES_NO_OPTION);
            if (ans == JOptionPane.YES_OPTION) {
                // Создаем файлы бэкапа с фильтрацией записей
                try (BufferedReader reader = new BufferedReader(new FileReader(new File(directory, fileName)));
                     BufferedWriter backupDbWriter = new BufferedWriter(new FileWriter(backupDbFile));
                     BufferedWriter backupIdWriter = new BufferedWriter(new FileWriter(backupIdFile))) {

                    String header = reader.readLine(); // Читаем заголовок
                    if (header != null) {
                        backupDbWriter.write(header);
                        backupDbWriter.newLine();
                    }

                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] fields = line.split(",");

                        // Проверяем, что ID корректен
                        if (fields.length > 0) {
                            int id = Integer.parseInt(fields[0]);

                            // Если запись не удалена, добавляем ее в бэкап
                            if (map.containsKey(id) && map.get(id) != -1L) {
                                backupDbWriter.write(line);
                                backupDbWriter.newLine();
                                backupIdWriter.write(id + "," + map.get(id));
                                backupIdWriter.newLine();
                            }
                        }
                    }

                    JOptionPane.showMessageDialog(null, "Бэкап базы данных успешно создан в директории: " + dbDirectory.getPath(),
                            "Успех", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "Ошибка при создании бэкапа базы данных: " + e.getMessage(),
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(null, "Сохранение базы данных отменено.",
                        "Отмена", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Ошибка при создании бэкапа базы данных: " + e.getMessage(),
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createBackup() {
        File projectRoot = new File(System.getProperty("user.dir")); // Корневая директория проекта
        File backupRootDirectory = new File(projectRoot, "backups");
        File backupDirectory = new File(backupRootDirectory,  directory + "_backup");
        File backupDbFile = new File(backupDirectory, fileName + ".backup");
        File backupIdFile = new File(backupDirectory, idFileName + ".backup");

        rewriteTo(backupRootDirectory, backupDirectory, backupDbFile, backupIdFile);
    }

    private void loadBackup() {
        String backupsDirPath = "backups"; // Корневая директория проекта
        File backupsDirectory = new File(backupsDirPath);
        File backupDirectory = new File(backupsDirectory, directory + "_backup");
        File backupDbFile = new File(backupDirectory, fileName + ".backup");
        File backupIdFile = new File(backupDirectory, idFileName + ".backup");
        File databaseFile = new File(directory, fileName); // Основной файл базы данных
        File dbDirectoryOld = new File(directory);

        if (!backupsDirectory.exists() || !backupsDirectory.isDirectory()) {
            JOptionPane.showMessageDialog(null, "Директория 'backups' не найдена.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!backupDirectory.exists() || !backupDirectory.isDirectory()) {
            JOptionPane.showMessageDialog(null, "Директория '" + directory + "_backup' не найдена.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!backupDbFile.exists() || !backupIdFile.exists()) {
            JOptionPane.showMessageDialog(null, "Ошибка: Не найдены файлы бэкапа базы данных или ID.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (!dbDirectoryOld.exists()) {
                if (dbDirectoryOld.mkdirs()) {
                    JOptionPane.showMessageDialog(null, "Директория '" + directory + "' успешно создана.", "Успех", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "Ошибка при создании директории '" + directory + "'.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return; // Выход из метода, если не удалось создать директорию
                }
            }

            // Проверяем существует ли база данных
            if (!databaseFile.exists()) {
                // Создаем файл базы данных
                if (databaseFile.createNewFile()) {
                    JOptionPane.showMessageDialog(null, "База данных успешно восстановлена: " + databaseFile.getPath(), "Успех", JOptionPane.INFORMATION_MESSAGE);

                    // Копируем содержимое из сохраненной базы данных в текущую базу данных
                    try (BufferedReader backupDbReader = new BufferedReader(new FileReader(backupDbFile));
                         BufferedWriter databaseWriter = new BufferedWriter(new FileWriter(databaseFile))) {

                        String line;
                        while ((line = backupDbReader.readLine()) != null) {
                            databaseWriter.write(line);
                            databaseWriter.newLine();
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Ошибка: не удалось восстановить файл базы данных.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // Загружаем ID из файла в хэш-мапу
            try (BufferedReader backupIdReader = new BufferedReader(new FileReader(backupIdFile))) {
                String line;
                while ((line = backupIdReader.readLine()) != null) {
                    String[] parts = line.split(",");

                    if (parts.length == 2) {
                        int id = Integer.parseInt(parts[0]);
                        long mappingValue = Long.parseLong(parts[1]);
                        map.put(id, mappingValue);
                    }
                }
                JOptionPane.showMessageDialog(null, "Восстановление базы данных завершено успешно.", "Успех", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Ошибка при восстановлении базы данных: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void searchById() {
        // Запрашиваем ID у пользователя
        String input = JOptionPane.showInputDialog(this, "Введите ID для поиска:", "Поиск записи по ID", JOptionPane.PLAIN_MESSAGE);
        if (input != null && !input.isEmpty()) {
            try {
                int searchId = Integer.parseInt(input); // Преобразуем ввод в целое число

                // Проверяем, существует ли ID и не удален ли он
                if (!map.containsKey(searchId) || map.get(searchId) == -1L) {
                    JOptionPane.showMessageDialog(this, "Запись с ID " + searchId + " не найдена или была удалена.", "Результат поиска", JOptionPane.INFORMATION_MESSAGE);
                    return; // Завершаем метод, если запись не найдена
                }

                // Если ID существует, ищем запись
                try (RandomAccessFile raf = new RandomAccessFile(new File(directory, fileName), "r")) {
                    raf.seek(map.get(searchId)); // Перемещаемся к смещению
                    String record = raf.readLine(); // Читаем запись
                    JOptionPane.showMessageDialog(this, "Найденная запись: " + record, "Результат поиска", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Ошибка при поиске записи: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                }

            } catch (NumberFormatException e) {
                // Обрабатываем случай, если ввод не является корректным целым числом
                JOptionPane.showMessageDialog(this, "Ошибка: Введите корректное целое число для ID.", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Ошибка: ID не может быть пустым!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void searchByField() {
        // Запрашиваем название столбца и значение у пользователя
        String columnName = JOptionPane.showInputDialog(this, "Введите название столбца для поиска:", "Поиск записи по полю", JOptionPane.PLAIN_MESSAGE);
        if (columnName == null || columnName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ошибка: Название столбца не может быть пустым!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
            return; // Завершаем метод, если название пустое
        }

        String value = JOptionPane.showInputDialog(this, "Введите значение для поиска:", "Поиск записи по полю", JOptionPane.PLAIN_MESSAGE);
        if (value == null || value.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ошибка: Значение не может быть пустым!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
            return; // Завершаем метод, если значение пустое
        }

        List<String> results = new ArrayList<>();

        // Поиск записей в файле
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(directory, fileName)))) {
            String header = reader.readLine(); // Читаем заголовок
            if (header == null) {
                JOptionPane.showMessageDialog(this, "Ошибка: Файл базы данных пуст.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return; // Завершаем метод, если файл пуст
            }

            String[] columnNames = header.split(",");
            int columnIndex = -1;

            // Ищем индекс столбца по его названию
            for (int i = 0; i < columnNames.length; i++) {
                if (columnNames[i].trim().equalsIgnoreCase(columnName)) {
                    columnIndex = i;
                    break;
                }
            }

            if (columnIndex == -1) {
                JOptionPane.showMessageDialog(this, "Ошибка: Столбец с названием \"" + columnName + "\" не найден.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return; // Завершаем метод, если столбец не найден
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");

                // Проверяем, что индекс поля корректен и значение совпадает
                if (columnIndex < fields.length && fields[columnIndex].equals(value)) {
                    results.add(line);
                }
            }

            // Проверяем, найдены ли результаты
            if (results.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Результаты поиска: ничего не найдено.", "Результат поиска", JOptionPane.INFORMATION_MESSAGE);
            } else {
                // Если результаты найдены, показываем их пользователю
                StringBuilder resultMessage = new StringBuilder("Найденные записи:\n");
                for (String result : results) {
                    resultMessage.append(result).append("\n");
                }
                JOptionPane.showMessageDialog(this, resultMessage.toString(), "Результат поиска", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Ошибка при чтении базы данных: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editRecord() {
        String input = JOptionPane.showInputDialog(this, "Введите ID для поиска:", "Поиск записи по ID", JOptionPane.PLAIN_MESSAGE);
        if (input != null && !input.isEmpty()) {
            try {
                int id = Integer.parseInt(input);
                // Проверка на существование ID
                if (!map.containsKey(id)) {
                    JOptionPane.showMessageDialog(null, "Ошибка: ID не найден: " + id, "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return; // Возвращаем false, если ID не найден
                }

                long offset = map.get(id);
                if (offset == -1L) {
                    JOptionPane.showMessageDialog(null, "Ошибка: Запись с ID " + id + " была удалена.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return; // Запись была удалена, нельзя редактировать
                }

                // Запрашиваем новое значение записи у пользователя
                String newRecord = JOptionPane.showInputDialog(null, "Введите новое значение записи для ID " + id + ":", "Редактирование записи", JOptionPane.PLAIN_MESSAGE);

                // Проверяем, что новое значение не пустое
                if (newRecord == null || newRecord.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Ошибка: Новая запись не может быть пустой!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try (RandomAccessFile raf = new RandomAccessFile(new File(directory, fileName), "rw")) {
                    raf.seek(offset); // Перемещаемся к смещению
                    raf.writeBytes(newRecord + System.lineSeparator()); // Записываем новую запись
                    JOptionPane.showMessageDialog(null, "Запись с ID " + id + " успешно отредактирована.", "Успех", JOptionPane.INFORMATION_MESSAGE);
                    loadDataFromCsvFile();
                    return; // Возвращаем true, если запись успешно отредактирована
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "Ошибка при редактировании записи: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return; // Возвращаем false в случае ошибки
                }
            } catch (NumberFormatException e) {
                // Обрабатываем случай, если ввод не является корректным целым числом
                JOptionPane.showMessageDialog(this, "Ошибка: Введите корректное целое число для ID.", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Ошибка: ID не может быть пустым!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Добавление новой записи в базу данных с проверкой на уникальность
    private void addRecord() {
        String record = JOptionPane.showInputDialog(this, "Введите запись для добавления:", "Добавление новой записи", JOptionPane.PLAIN_MESSAGE);
        // Разделяем запись на поля для проверки уникальности
        String[] fields = record.split(",");
        int key = Integer.parseInt(fields[0]);

        // Проверка на уникальность ID
        if (map.containsKey(key)) {
            JOptionPane.showMessageDialog(null, "Ошибка: Дубликат ID: " + key, "Ошибка", JOptionPane.ERROR_MESSAGE);
            return; // Возвращаем false, если ID уже существует
        }

        // Добавление записи в файл
        try (RandomAccessFile raf = new RandomAccessFile(new File(directory, fileName), "rw")) {
            raf.seek(raf.length()); // Перемещаемся в конец файла
            long offset = raf.getFilePointer(); // Получаем текущее смещение
            raf.writeBytes(record + System.lineSeparator()); // Записываем запись
            map.put(key, offset); // Обновляем позицию в мапе
            JOptionPane.showMessageDialog(null, "Запись успешно добавлена.", "Успех", JOptionPane.INFORMATION_MESSAGE);
            tableModel.addRow(fields);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Ошибка при добавлении записи: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Удаление записи по ID
    private void deleteRecordById() {
        String input = JOptionPane.showInputDialog(this, "Введите ID для удаления:", "Удаление записи", JOptionPane.PLAIN_MESSAGE);
        if (input != null && !input.isEmpty()) {
            try {
                int id = Integer.parseInt(input);
                // Теперь можно использовать id, например, для удаления записи
                if (map.containsKey(id)) {
                    map.put(id, -1L); // Заменяем ID на -1, чтобы обозначить, что запись удалена
                    JOptionPane.showMessageDialog(null, "Запись с ID " + id + " успешно удалена.", "Успех", JOptionPane.INFORMATION_MESSAGE);
                    loadDataFromCsvFile();
                } else {
                    JOptionPane.showMessageDialog(null, "Ошибка: ID не найден для удаления: " + id, "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Ошибка: Введите корректное целое число для ID.", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Ошибка: ID не может быть пустым!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
        }

    }

    private void deleteRecordByField() {
        // Запрашиваем название столбца и значение у пользователя
        String columnName = JOptionPane.showInputDialog(this, "Введите название столбца для удаления:", "Удаление записи по полю", JOptionPane.PLAIN_MESSAGE);
        if (columnName == null || columnName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ошибка: Название столбца не может быть пустым!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
            return; // Завершаем метод, если название пустое
        }

        String value = JOptionPane.showInputDialog(this, "Введите значение в столбце для удаления:", "Удаление записи по полю", JOptionPane.PLAIN_MESSAGE);
        if (value == null || value.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ошибка: Значение не может быть пустым!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
            return; // Завершаем метод, если значение пустое
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(new File(directory, fileName)))) {
            String header = reader.readLine(); // Читаем заголовок
            if (header == null) {
                JOptionPane.showMessageDialog(this, "Ошибка: Файл базы данных пуст.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String[] columnNames = header.split(",");
            int columnIndex = -1;

            // Ищем индекс столбца по его названию
            for (int i = 0; i < columnNames.length; i++) {
                if (columnNames[i].trim().equalsIgnoreCase(columnName)) {
                    columnIndex = i;
                    break;
                }
            }

            if (columnIndex == -1) {
                JOptionPane.showMessageDialog(this, "Ошибка: Столбец с названием \"" + columnName + "\" не найден.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String line;
            boolean recordDeleted = false; // Флаг для отслеживания, была ли удалена хотя бы одна запись
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");

                // Проверяем, что индекс поля корректен и значение совпадает
                if (columnIndex < fields.length && fields[columnIndex].equals(value)) {
                    // Получаем ID записи (предположим, что ID находится в первом столбце)
                    int id = Integer.parseInt(fields[0]);

                    // Устанавливаем оффсет в хэш-мапе на -1L
                    if (map.containsKey(id) && map.get(id) != -1L) {
                        map.put(id, -1L);
                        recordDeleted = true; // Запись была удалена
                    } else {
                        JOptionPane.showMessageDialog(this, "Ошибка: ID " + id + " не найден в хэш-мапе.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }

            if (recordDeleted) {
                JOptionPane.showMessageDialog(this, "Записи с полем \"" + columnName + "\" и значением \"" + value + "\" успешно удалены.", "Успех", JOptionPane.INFORMATION_MESSAGE);
                loadDataFromCsvFile();
            } else {
                JOptionPane.showMessageDialog(this, "Записи с полем \"" + columnName + "\" и значением \"" + value + "\" не найдены.", "Информация", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Ошибка при чтении базы данных: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: ID должен быть числом. " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadDataFromCsvFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(directory, fileName)))) {
            String line;
            tableModel.setRowCount(0);
            tableModel.setColumnIdentifiers(new Object[0]);

            String headerLine = reader.readLine();
            tableModel.setColumnIdentifiers(headerLine.split(","));

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                int id = Integer.parseInt(data[0]);
                if (map.containsKey(id) && map.get(id) != -1L) {
                    tableModel.addRow(data);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Ошибка при загрузке данных из файла: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void importToExcel() {
        // Выбираем путь для сохранения Excel файла
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить как Excel");
        fileChooser.setSelectedFile(new File("database.xlsx"));
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File excelFile = fileChooser.getSelectedFile();

            if (!excelFile.getName().toLowerCase().endsWith(".xlsx")) {
                excelFile = new File(excelFile.getAbsolutePath() + ".xlsx");
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(new File(directory, fileName)));
                 FileOutputStream fos = new FileOutputStream(excelFile)) {

                Workbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("Database");

                String line;
                int rowNum = 0;

                while ((line = reader.readLine()) != null) {
                    String[] fields = line.split(",");
                    Row row = sheet.createRow(rowNum);
                    rowNum++;

                    for (int colNum = 0; colNum < fields.length; colNum++) {
                        Cell cell = row.createCell(colNum);
                        cell.setCellValue(fields[colNum].trim());
                    }
                }

                for (int colNum = 0; colNum < sheet.getRow(0).getLastCellNum(); colNum++) {
                    sheet.autoSizeColumn(colNum);
                }

                workbook.write(fos);
                workbook.close();

                JOptionPane.showMessageDialog(this, "Данные успешно импортированы в Excel:\n" + excelFile.getAbsolutePath(),
                        "Импорт завершен", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Ошибка при импорте в Excel: " + e.getMessage(),
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

}

