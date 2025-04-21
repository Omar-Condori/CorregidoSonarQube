package pe.edu.upeu.sysalmacen.control;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject; // Corregido: el import correcto es de org.json
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.upeu.sysalmacen.dtos.report.ProdMasVendidosDTO;
import pe.edu.upeu.sysalmacen.modelo.MediaFile;
import pe.edu.upeu.sysalmacen.servicio.IMediaFileService;
import pe.edu.upeu.sysalmacen.servicio.IProductoService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/reporte")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final IProductoService productoService;
    private final IMediaFileService mfService;
    private final Cloudinary cloudinary;

    @GetMapping("/pmvendidos")
    public List<ProdMasVendidosDTO> getProductosMasVendidos() {
        return productoService.obtenerProductosMasVendidos();
    }

    @GetMapping(value = "/generateReport", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> generateReport() {
        try {
            byte[] data = productoService.generateReport();
            return ResponseEntity.ok(data);
        } catch (IOException e) {
            log.error("Error al generar el reporte", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/readFile/{idFile}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> readFile(@PathVariable("idFile") Long idFile) {
        try {
            byte[] data = mfService.findById(idFile).getContent();
            return ResponseEntity.ok(data);
        } catch (IOException e) {
            log.error("Error al leer el archivo con ID {}", idFile, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/saveFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> saveFile(@RequestParam("file") MultipartFile multipartFile) {
        try {
            MediaFile mf = new MediaFile();
            mf.setContent(multipartFile.getBytes());
            mf.setFileName(multipartFile.getOriginalFilename());
            mf.setFileType(multipartFile.getContentType());
            mfService.save(mf);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            log.error("Error al guardar archivo en base de datos", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/saveFileCloud", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> saveFileCloud(@RequestParam("file") MultipartFile multipartFile) {
        try {
            File f = this.convertToFile(multipartFile);
            Map<String, Object> response = cloudinary.uploader().upload(f, ObjectUtils.asMap("resource_type", "auto"));
            JSONObject json = new JSONObject(response);
            String url = json.getString("url");
            log.info("Archivo subido a Cloudinary: {}", url);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error al guardar archivo en Cloudinary", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private File convertToFile(MultipartFile multipartFile) throws IOException {
        File file = new File(multipartFile.getOriginalFilename());
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(multipartFile.getBytes());
        }
        return file;
    }
}
