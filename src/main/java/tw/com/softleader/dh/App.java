package tw.com.softleader.dh;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.controlsfx.control.MaskerPane;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import tw.com.softleader.dh.basic.Config;
import tw.com.softleader.dh.basic.SimpleAlert;
import tw.com.softleader.dh.basic.TomcatType;
import tw.com.softleader.dh.basic.VerifyException;
import tw.com.softleader.dh.basic.node.TimeSpinner;
import tw.com.softleader.dh.strategy.BackupHandler;
import tw.com.softleader.dh.strategy.DeployHandler;
import tw.com.softleader.dh.strategy.RemarkHandler;

public class App extends Application {

	private ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void start(final Stage stage) throws IOException {
		stage.setTitle("DeployHelper");

		final Config config = readConfig();

		// ===佈署模組===
		final DeployHandler deployHandler = new DeployHandler(config);
		// ===備份模組===
		final BackupHandler backupHandler = new BackupHandler(config);
		// ===註記模組===
		final RemarkHandler remarkHandler = new RemarkHandler(config);

		// ===元素準備===
		final CheckBox backupCheckBox = new CheckBox("佈署完成後仍保留備份檔案");

		// Tomcat類型選擇區塊
		final ToggleGroup tomcatType = new ToggleGroup();
		final List<RadioButton> rbs = Stream.of(TomcatType.values()).map(type -> new RadioButton(type.toString())).collect(Collectors.toList());
		rbs.forEach(rb -> rb.setToggleGroup(tomcatType));
		final TextField serviceNameTextField = new TextField();

		// 檔案選擇視窗-war
		final FileChooser warChooser = new FileChooser();
		warChooser.getExtensionFilters().add(new ExtensionFilter("佈署檔", "*.war"));
		// 檔案選擇按鈕
		final Button openWarButton = new Button("選擇要佈署的War檔");
		openWarButton.setPrefWidth(200);
		final TextField warPathTextField = new TextField();
		warPathTextField.setEditable(false);
		warPathTextField.setPrefWidth(400);

		// 資料夾選擇視窗
		final DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setTitle("請選擇Tomcat位置");
		// Tomcat路徑選擇按鈕
		final Button openTomcatButton = new Button("選擇要佈署的路徑");
		openTomcatButton.setPrefWidth(200);
		final TextField tomcatPathTextField = new TextField();
		tomcatPathTextField.setEditable(false);
		tomcatPathTextField.setPrefWidth(400);

		// 備份路徑選擇按鈕
		final Button openBackupButton = new Button("選擇要備份的路徑");
		openBackupButton.setPrefWidth(200);
		final TextField backupPathTextField = new TextField();
		backupPathTextField.setEditable(false);
		backupPathTextField.setPrefWidth(400);

		// 還原按鈕
		final Button restoreButton = new Button("還原為指定版本");

		// 佈署按鈕
		final Button deployButton = new Button("開始佈署");
		deployButton.setDefaultButton(true);

		// 預約佈署按鈕
		final Button bookDeployButton = new Button("預約佈署");
		final DatePicker bookDate = new DatePicker(LocalDate.now());
		final TimeSpinner bookTime = new TimeSpinner(LocalTime.now(), DateTimeFormatter.ofPattern("HH:mm"));

		// 備份紀錄列表
		final ListView<String> backupHistory = new ListView<>();
		backupHistory.setPrefWidth(400);
		backupHistory.setPrefHeight(200);

		// 備份註記輸入框
		final TextArea backupRemark = new TextArea();
		backupRemark.setPrefWidth(350);
		backupRemark.setPrefHeight(200);
		backupRemark.setDisable(true);
		// 備份註記儲存按鈕
		final Button backupRemarkButton = new Button("儲存(ctrl+s)");
		backupRemarkButton.setPrefWidth(350);

		// ===畫面組製===

		// 參數勾選區塊
		final GridPane propertiesPane = new GridPane();
		final List<Node> rbNodes = new ArrayList<>();
		for (int i = 0; i < rbs.size(); i++) {
			if (TomcatType.service.toString().equals(rbs.get(i).getText())) {
				final GridPane serviceTypePane = new GridPane();
				GridPane.setConstraints(rbs.get(i), 0, 0);
				GridPane.setConstraints(serviceNameTextField, 1, 0);
				serviceTypePane.getChildren().addAll(rbs.get(i), serviceNameTextField);
				GridPane.setConstraints(serviceTypePane, i, 0);
				rbNodes.add(serviceTypePane);
			} else {
				GridPane.setConstraints(rbs.get(i), i, 0);
				rbNodes.add(rbs.get(i));
			}
		}
		GridPane.setConstraints(backupCheckBox, 0, 1);
		propertiesPane.setHgap(6);
		propertiesPane.setVgap(6);
		propertiesPane.getChildren().addAll(rbNodes);
		propertiesPane.getChildren().addAll(backupCheckBox);

		// 檔案選擇器區塊
		final GridPane fileChooserPane = new GridPane();
		GridPane.setConstraints(openWarButton, 0, 0);
		GridPane.setConstraints(warPathTextField, 1, 0);
		GridPane.setConstraints(openTomcatButton, 0, 1);
		GridPane.setConstraints(tomcatPathTextField, 1, 1);
		GridPane.setConstraints(openBackupButton, 0, 2);
		GridPane.setConstraints(backupPathTextField, 1, 2);
		fileChooserPane.setHgap(6);
		fileChooserPane.setVgap(6);
		fileChooserPane.getChildren().addAll(openWarButton, warPathTextField, openTomcatButton, tomcatPathTextField, openBackupButton, backupPathTextField);

		// 備份註記區塊
		final GridPane backupRemarkPane = new GridPane();
		backupRemarkPane.setHgap(6);
		backupRemarkPane.setVgap(6);
		GridPane.setConstraints(backupRemark, 0, 0);
		GridPane.setConstraints(backupRemarkButton, 0, 1);
		backupRemarkPane.getChildren().addAll(backupRemark, backupRemarkButton);

		// 備份列表區塊
		final GridPane backupHistoryPane = new GridPane();
		backupHistoryPane.setHgap(6);
		backupHistoryPane.setVgap(6);
		GridPane.setConstraints(backupHistory, 0, 0);
		GridPane.setConstraints(backupRemarkPane, 1, 0);
		backupHistoryPane.getChildren().addAll(backupHistory, backupRemarkPane);

		// 動作按鈕區塊
		final GridPane actionPane = new GridPane();
		actionPane.setHgap(6);
		actionPane.setVgap(6);
		GridPane.setConstraints(restoreButton, 0, 0);
		GridPane.setConstraints(deployButton, 1, 0);
		GridPane.setConstraints(bookDeployButton, 3, 0);
		GridPane.setConstraints(bookDate, 4, 0);
		GridPane.setConstraints(bookTime, 5, 0);
		actionPane.getChildren().addAll(restoreButton, deployButton, bookDeployButton, bookDate, bookTime);

		// 燈箱區塊
		final MaskerPane masker = new MaskerPane();
		masker.setVisible(false);
		final Pane root = new VBox(12);
		root.getChildren().addAll(propertiesPane, fileChooserPane, backupHistoryPane, actionPane);
		root.setPadding(new Insets(12, 12, 12, 12));
		final StackPane body = new StackPane(root, masker);

		// ===元素事件===
		rbs.forEach(rb -> {
			if (TomcatType.service.toString().equals(rb.getText())) {
				rb.setOnAction(e -> {
					serviceNameTextField.setVisible(true);
					config.setTomcatType(TomcatType.valueOf(rb.getText()));
				});
			} else {
				rb.setOnAction(e -> {
					serviceNameTextField.setVisible(false);
					config.setTomcatType(TomcatType.valueOf(rb.getText()));
				});
			}
		});

		// Tomcat service輸入
		serviceNameTextField.textProperty().addListener((observable, oldValue, newValue) -> config.setTomcatServiceName(newValue));

		// war檔選擇
		openWarButton.setOnAction(e -> {
			warChooser.setInitialDirectory(null);
			Optional.ofNullable(warPathTextField.getText())
				.map(File::new)
				.filter(File::exists)
				.map(File::getParentFile)
				.ifPresent(warChooser::setInitialDirectory);
			final File file = warChooser.showOpenDialog(stage);
			if (file != null) {
				warPathTextField.setText(file.getPath());
				config.setWarPath(file.getPath());
			}
		});

		// 佈署路徑選擇
		openTomcatButton.setOnAction(e -> {
			warChooser.setInitialDirectory(null);
			Optional.ofNullable(tomcatPathTextField.getText())
				.map(File::new)
				.filter(File::exists)
				.map(File::getParentFile)
				.ifPresent(dirChooser::setInitialDirectory);
			final File file = dirChooser.showDialog(stage);
			if (file != null) {
				tomcatPathTextField.setText(file.getPath());
				config.setTomcatPath(file.getPath());
			}
		});

		// 備份路徑選擇
		openBackupButton.setOnAction(e -> {
			warChooser.setInitialDirectory(null);
			Optional.ofNullable(backupPathTextField.getText())
			.map(File::new)
			.filter(File::exists)
			.map(File::getParentFile)
			.ifPresent(dirChooser::setInitialDirectory);
			final File file = dirChooser.showDialog(stage);
			if (file != null) {
				backupPathTextField.setText(file.getPath());
				config.setBackupPath(file.getPath());
				reloadHistory(backupHandler, backupHistory);
			}
		});

		// 佈署
		deployButton.setOnAction(e -> {
			config.setKeepBackUpFile(backupCheckBox.isSelected());
			try {
				masker.setText("準備開始進行佈署...");
				disable(masker);
				deployHandler.deploy(
						log -> Platform.runLater(() -> masker.setText(log)),
						t -> Platform.runLater(() -> SimpleAlert.error("佈署期間發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", t)),
						() -> {
							enable(masker);
							reloadHistory(backupHandler, backupHistory);
							Platform.runLater(() -> SimpleAlert.info("已於 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " 完成佈署"));
							saveConfig(config);
						}
				);
			} catch (final VerifyException ex) {
				SimpleAlert.warn(ex.getMsgs());
				enable(masker);
				ex.printStackTrace();
			} catch (final Exception ex) {
				SimpleAlert.error("佈署期間發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", ex);
				enable(masker);
				ex.printStackTrace();
			}
		});

		// 預約佈署
		bookDeployButton.setOnAction(e -> {
			config.setKeepBackUpFile(backupCheckBox.isSelected());
			try {
				masker.setText("準備開始進行佈署...");
				disable(masker);
				final LocalDate date = bookDate.getValue();
				final LocalTime time = bookTime.getValue();
				deployHandler.book(
						date.atTime(time),
						log -> Platform.runLater(() -> masker.setText(log)),
						t -> Platform.runLater(() -> SimpleAlert.error("佈署期間發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", t)),
						() -> {
							enable(masker);
							reloadHistory(backupHandler, backupHistory);
							Platform.runLater(() -> SimpleAlert.info("已於 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " 完成佈署"));
							saveConfig(config);
						}
				);
			} catch (final VerifyException ex) {
				SimpleAlert.warn(ex.getMsgs());
				enable(masker);
				ex.printStackTrace();
			} catch (final Exception ex) {
				SimpleAlert.error("佈署期間發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", ex);
				enable(masker);
				ex.printStackTrace();
			}
		});

		// 還原
		restoreButton.setOnAction(e -> {
			backupHandler.setChooseFileName(backupHistory.getSelectionModel().getSelectedItem());
			try {
				masker.setText("準備開始進行還原...");
				disable(masker);
				backupHandler.restore(
						log -> Platform.runLater(() -> masker.setText(log)),
						t -> Platform.runLater(() -> SimpleAlert.error("還原期間發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", t)),
						() -> {
							enable(masker);
							Platform.runLater(() -> SimpleAlert.info("已於 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " 完成還原"));
							saveConfig(config);
						}
				);
			} catch (final VerifyException ex) {
				SimpleAlert.warn(ex.getMsgs());
				enable(masker);
				ex.printStackTrace();
			} catch (final Exception ex) {
				SimpleAlert.error("還原期間發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", ex);
				enable(masker);
				ex.printStackTrace();
			}
		});

		// 備份檔案選擇
		backupHistory.setOnMouseClicked(e -> {
			try {
				masker.setText("讀取備份註記中...");
				disable(masker, backupRemarkButton, backupRemark);
				remarkHandler.loadRemark(
					e,
					t -> Platform.runLater(() -> SimpleAlert.error("讀取註記發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", t)),
					text -> Platform.runLater(() -> {
						backupRemark.setText(text);
						enable(masker, backupRemarkButton, backupRemark);
					}),
					() -> enable(masker)
				);
			} catch (final VerifyException ex) {
				SimpleAlert.warn(ex.getMsgs());
				enable(masker);
				ex.printStackTrace();
			} catch (final Exception ex) {
				SimpleAlert.error("讀取註記發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", ex);
				enable(masker);
				ex.printStackTrace();
			}
		});

		// 備份註記儲存
		backupRemarkButton.setOnAction(e -> {
			try {
				masker.setText("儲存備份註記中...");
				disable(masker, backupRemarkButton, backupRemark);
				remarkHandler.saveRemark(
					backupRemark.getText(),
					backupHistory.getSelectionModel().getSelectedItem(),
					t -> Platform.runLater(() -> {
						SimpleAlert.error("儲存註記發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", t);
						enable(masker, backupRemarkButton, backupRemark);
					}),
					() -> enable(masker, backupRemarkButton, backupRemark)
				);
			} catch (final VerifyException ex) {
				SimpleAlert.warn(ex.getMsgs());
				enable(masker, backupRemarkButton, backupRemark);
				ex.printStackTrace();
			} catch (final Exception ex) {
				SimpleAlert.error("儲存註記發生預期外的錯誤\n請擷取以下訊息並通報系統管理員", ex);
				enable(masker, backupRemarkButton, backupRemark);
				ex.printStackTrace();
			}
		});

		// 備份註記儲存(快捷鍵)
		backupRemark.setOnKeyPressed(e -> {
			if (e.isShortcutDown() && e.isControlDown() && KeyCode.S.equals(e.getCode())) {
				backupRemarkButton.fire();
			}
		});

		// 視窗關閉
		stage.setOnCloseRequest(e -> System.exit(0));

		// 帶入預設值
		Optional.ofNullable(config.getTomcatType()).ifPresent(type -> rbs.forEach(rb -> {
            if (type.toString().equals(rb.getText())) {
                rb.fire();
            }
        }));
		Optional.ofNullable(config.getTomcatServiceName()).ifPresent(serviceNameTextField::setText);
		Optional.ofNullable(config.isKeepBackUpFile()).ifPresent(backupCheckBox::setSelected);
		Optional.ofNullable(config.getWarPath()).ifPresent(warPathTextField::setText);
		Optional.ofNullable(config.getTomcatPath()).ifPresent(tomcatPathTextField::setText);
		Optional.ofNullable(config.getBackupPath()).ifPresent(backupPathTextField::setText);

		reloadHistory(backupHandler, backupHistory);

		final Scene scene = new Scene(body);
		Optional.ofNullable(getClass().getClassLoader().getResource("application.css")).map(URL::toExternalForm).ifPresent(scene.getStylesheets()::add);
		stage.setScene(scene);
		stage.show();
	}


	private final static File CONFIG_FILE = new File("config.json");

	private Config readConfig() {
		Config config;

		Config dummy = new Config();
		if (!CONFIG_FILE.exists()) {
			try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(CONFIG_FILE))) {
				osw.write(objectMapper.writeValueAsString(dummy));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		try(InputStream configStream = new FileInputStream(CONFIG_FILE)) {
			config = objectMapper.readValue(configStream, Config.class);
			config.verify();
		} catch (final VerifyException ex) {
			config = dummy;
			SimpleAlert.warn(ex.getMsgs());
			ex.printStackTrace();
		} catch (final Exception ex) {
			config = dummy;
			SimpleAlert.error("讀取設定檔發生錯誤\n請擷取以下訊息並通報系統管理員", ex);
			ex.printStackTrace();
		}
		return config;
	}

	private void saveConfig(Config config) {
		try {
			objectMapper.writeValue(CONFIG_FILE, config);
		} catch (final IOException e) {
			Platform.runLater(() -> SimpleAlert.error("儲存設定檔時發生異常", e));
		}
	}

	private void reloadHistory(final BackupHandler backupHandler, final ListView<String> backupHistory) {
		backupHandler.reload();
		final List<String> fileNames = backupHandler.getBackupFiles().stream().map(File::getName).collect(Collectors.toList());
		Platform.runLater(() -> backupHistory.setItems(FXCollections.observableArrayList(fileNames)));
	}

	private void disable(final MaskerPane masker, final Node... nodes) {
		masker.setVisible(true);
		for (final Node node : nodes) {
			node.setDisable(true);
		}
	}

	private void enable(final MaskerPane masker, final Node... nodes) {
		masker.setVisible(false);
		for (final Node node : nodes) {
			node.setDisable(false);
		}
	}

	public static void main(final String[] args) throws IOException {
		App.launch(args);
	}

}
