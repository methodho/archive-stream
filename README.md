# Archive Stream

This library integrate apache-compress into Java Stream API. Read on to see if it's right for you

## Usage

### Compress

```java
Map<ZipArchiveEntry, InputStream> archives = new HashMap<>();

// Put an empty dir, end with '/'
archives.put(new ZipArchiveEntry("/an/empty/dir/"), null);

// Put form a file
File fromFile = ...
archives.put(new ZipArchiveEntry("/from/some-file.txt"), new FileInputStream(fromFile));

// Put byte array
byte[] someBytes = ...
archives.put(new ZipArchiveEntry("/from/some-bytes"), new ByteArrayInputStream(someBytes));

// ... add more archive to compress

File zip = ... // your zip file
ArchiveStream.of(new FileOutputStream(zip)).compress(archives);
```

It will auto detected the archive type by the first entry.key of map:

- `org.apache.commons.compress.archivers.ar.ArArchiveEntry`
- `org.apache.commons.compress.archivers.zip.ZipArchiveEntry`
- `org.apache.commons.compress.archivers.tar.TarArchiveEntry`
- `org.apache.commons.compress.archivers.jar.JarArchiveEntry`
- `org.apache.commons.compress.archivers.cpio.CpioArchiveEntry`

Or you can detemine the type on your own:

```java
Map<ArchiveEntry, InputStream> archives = new HashMap<>();
// ...
File zip = ... // your zip file
OutputStream out = new ArchiveStreamFactory()
            .createArchiveOutputStream(ArchiveStreamFactory.JAR, new FileOutputStream(zip));
ArchiveStream.of(out).compress(archives);
```

### Decompress

Let it auto detect the archive type to decompress

```java
File zip = ... // your zip file
InputStream zipIn = new BufferedInputStream(new FileInputStream(zip));
ArchiveStream.of(zipIn)...
```

You can detemine the type as well:

```java
File zip = ... // your zip file
InputStream zipIn = new ArchiveStreamFactory()
            .createArchiveInputStream(ArchiveStreamFactory.JAR, 
                    new BufferedInputStream(new FileInputStream(zip)));
ArchiveStream.of(zipIn)...
```

Use `mapToObj(...)` to map to `java.util.Stream`:

```java
File zip = ... // your zip file
InputStream zipIn = new BufferedInputStream(new FileInputStream(zip));
List<MyObject> content = ArchiveStream.of(zipIn)
            .mapToObj((archiveEntry, archiveEntryIn) -> {...})
            ...
```

## Example

- Downloading a file from spring controllers

```java
@Controller
public class MyController {

  @Autowired
  private MyService myService;

  @RequestMapping(value = "/zip", method = RequestMethod.POST, produces = "application/zip")
  public void zip(HttpServletResponse response)
      throws IOException, ArchiveException {
    Map<ZipArchiveEntry, InputStream> archives = myService.getArchives();
    ArchiveStream.of(response.getOutputStream()).compress(archives);
  }
}
```

- Uploading a file to spring controllers


```java
@Controller
public class MyController {

	public static final String ROOT = "some-upload-dir";
	
	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public String upload(@RequestParam("file") MultipartFile file) {
	  if (!file.isEmpty()) {
	    try {
	      BufferedInputStream in = new BufferedInputStream(file.getInputStream());
	      ArchiveStream.of(in).forEach((archiveEntry, archiveEntryIn) -> {
	        Path path = Paths.get(ROOT, file.getOriginalFilename(), entry.getName());
	        if (archiveEntry.isDirectory()) {
	          Files.createDirectories(path);
	        } else {
	          Files.createDirectories(path.getParent());
	          Files.copy(archiveEntryIn, path, StandardCopyOption.REPLACE_EXISTING);
	        }
	      });
	    } catch (IOException | ArchiveException | RuntimeException e) {
	      log.error("Something went wrong when extracting [{}]", file.getOriginalFilename(), e);
	    }
	  } else {
	    log.error("Failed to upload [{}] because it was empty", file.getOriginalFilename());
	  }
	
	  return "redirect:/some-path";
	}
}
```
