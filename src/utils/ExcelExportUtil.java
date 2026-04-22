package utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;

public final class ExcelExportUtil {

    private ExcelExportUtil(){
    }

    public static void exportTable(Component parent, JTable table, String defaultFileName, String sheetName){

        if(table == null || table.getRowCount() == 0){
            JOptionPane.showMessageDialog(parent, "There is no data to export.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Excel File");
        chooser.setSelectedFile(new File(defaultFileName));
        chooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        int result = chooser.showSaveDialog(parent);

        if(result != JFileChooser.APPROVE_OPTION){
            return;
        }

        File file = chooser.getSelectedFile();

        // If user picks a folder, save default file name inside that folder.
        if(file != null && file.isDirectory()){
            file = new File(file, defaultFileName);
        }

        if(!file.getName().toLowerCase().endsWith(".xlsx")){
            file = new File(file.getAbsolutePath() + ".xlsx");
        }

        if(file.exists()){
            int overwrite = JOptionPane.showConfirmDialog(
                parent,
                "File already exists. Overwrite?\n" + file.getAbsolutePath(),
                "Confirm Overwrite",
                JOptionPane.YES_NO_OPTION
            );

            if(overwrite != JOptionPane.YES_OPTION){
                return;
            }
        }

        TableModel model = table.getModel();

        try(
                Workbook workbook = new XSSFWorkbook();
                FileOutputStream outputStream = new FileOutputStream(file)
        ){
            Sheet sheet = workbook.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);

            for(int col = 0; col < model.getColumnCount(); col++){
                headerRow.createCell(col).setCellValue(model.getColumnName(col));
            }

            for(int rowIndex = 0; rowIndex < model.getRowCount(); rowIndex++){
                Row row = sheet.createRow(rowIndex + 1);

                for(int col = 0; col < model.getColumnCount(); col++){
                    Object value = model.getValueAt(rowIndex, col);
                    Cell cell = row.createCell(col);
                    setTypedCellValue(cell, value);
                }
            }

            for(int col = 0; col < model.getColumnCount(); col++){
                sheet.autoSizeColumn(col);
            }

            workbook.write(outputStream);

            JOptionPane.showMessageDialog(parent, "Excel exported successfully to:\n" + file.getAbsolutePath());

        }catch(Exception e){
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent,
                "Export to Excel failed.\nPlease close the file if it is already open and try again.");
        }
    }

    private static void setTypedCellValue(Cell cell, Object value){

        if(value == null){
            cell.setBlank();
            return;
        }

        if(value instanceof Number){
            cell.setCellValue(((Number) value).doubleValue());
            return;
        }

        if(value instanceof Boolean){
            cell.setCellValue((Boolean) value);
            return;
        }

        cell.setCellValue(value.toString());
    }
}