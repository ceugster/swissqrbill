package ch.eugster.swissqrbill.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.eugster.swissqrbill.SwissQRBillGenerator;
import net.codecrete.qrbill.generator.GraphicsFormat;
import net.codecrete.qrbill.generator.Language;
import net.codecrete.qrbill.generator.OutputSize;

public class QRCodeTest 
{
	private String output;

	private String invoice;

	@BeforeEach
	public void beforeEach() throws URISyntaxException, IOException
	{
		String out = System.getProperty("user.home") + File.separator + UUID.randomUUID() + ".pdf";
		this.output = new File(out).toURI().toASCIIString();
		URI uri = QRCodeTest.class.getResource("/invoice.pdf").toURI();
		File source = Paths.get(uri).toFile();
		this.invoice = System.getProperty("user.home") + File.separator + "Documents/invoice.pdf";
		FileUtils.copyFile(source, new File(this.invoice));
	}

	@AfterEach
	public void afterEach()
	{
		Path path = null;
		try
		{
			URI uri = new URI(this.output);
			path = Paths.get(uri);
		}
		catch (Exception e)
		{
			if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
			{
				File file = new File(this.output);
				if (file.isAbsolute() && this.output.startsWith("/"))
				{
					this.output = this.output.substring(1);
				}
			}
			path = Paths.get(this.output);
		}
		if (path != null && path.toFile().exists())
		{
			path.toFile().delete();
		}
		File file = new File(this.invoice);
		if (file.exists())
		{
			file.delete();
		}
	}
	
	@Test
	public void showVolumes()
	{
		Path path = null;
		if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
		{
			String filepath = "/C:/Users/christian/";
			File file = new File(filepath);
			assertTrue(file.exists());
			assertTrue(file.isAbsolute());
			assertTrue(file.isDirectory());
			if (file.getAbsolutePath().startsWith("/"))
			{
				file = new File(file.getAbsolutePath().substring(1));
			}
			path = Paths.get(file.getAbsolutePath());
			assertTrue(path.toFile().exists());
			assertTrue(path.isAbsolute());
			assertTrue(path.toFile().isDirectory());
		}
		else if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0)
		{
			path = Paths.get("/Festplatte", "Users", "christian");
			assertFalse(path.toFile().exists());
			assertTrue(path.isAbsolute());
			assertFalse(path.toFile().isDirectory());
			if (path.getName(0).equals(Paths.get("Festplatte")))
			{
				path = Paths.get("/", path.subpath(1, path.getNameCount()).toString());
				System.out.println(path.toString());
			}
			assertTrue(path.toFile().exists());
			assertTrue(path.isAbsolute());
			assertTrue(path.toFile().isDirectory());
		}
	}
	
	@Test
	public void testWithInvoiceAsNumberAndDebtorNumber() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result);	
	}
	
	@Test
	public void testWithReference() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("invoice", 12345);
		node.put("currency", "CHF");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		node.put("reference", "123451234567");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result);	
	}
	
	@Test
	public void testWithoutExistingInvoiceToAppendTo() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", "12345");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result);	
	}
	
	@Test
	public void testMissingCreditor() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("invoice", "12345");
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("reference", "3139471430009017");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", "1234567");
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode resultNode = mapper.readTree(result.toString());
		assertEquals(ArrayNode.class, resultNode.getClass());
		ArrayNode arrayNode = ArrayNode.class.cast(resultNode);
		assertEquals(4, arrayNode.size());
		Iterator<JsonNode> entries = arrayNode.elements();
		while (entries.hasNext())
		{
			ObjectNode next = ObjectNode.class.cast(entries.next());
			Iterator<String> fields = next.fieldNames();
			while (fields.hasNext())
			{
				String fieldname = fields.next();
				assertEquals("12345", fieldname);
				if ("'creditor.name' muss den Namen des Rechnungstellers enthalten (maximal 70 Buchstaben).".equals(next.get(fieldname).asText()))
				{
					assertTrue(true);
				}
				else if ("'creditor.address' muss die Adresse des Rechnungstellers enthalten (maximal 70 Buchstaben).".equals(next.get(fieldname).asText()))
				{
					assertTrue(true);
				}
				else if ("'creditor.city' muss Postleitzahl und Ort des Rechnungstellers enthalten (maximal 70 Buchstaben).".equals(next.get(fieldname).asText()))
				{
					assertTrue(true);
				}
				else if ("'creditor.country' muss den zweistelligen Landcode gemäss ISO 3166 des Rechnungstellers enthalten.".equals(next.get(fieldname).asText()))
				{	
					assertTrue(true);
				}
				else
				{
					assertTrue(false);
				}
			}
		}
	}
	
	@Test
	public void testMissingDebtor() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("invoice", "12345");
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("reference", "3139471430009017");
		node.put("message", "Abonnement für 2020");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result.toString());
	}
	
	@Test
	public void testWithoutInvoice() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("iban", "CH4431999123000889012");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode resultNode = mapper.readTree(result.toString());
		assertEquals(ArrayNode.class, resultNode.getClass());
		ArrayNode arrayNode = ArrayNode.class.cast(resultNode);
		assertEquals(1, arrayNode.size());
		Iterator<JsonNode> entries = arrayNode.elements();
		while (entries.hasNext())
		{
			ObjectNode next = ObjectNode.class.cast(entries.next());
			Iterator<String> fields = next.fieldNames();
			while (fields.hasNext())
			{
				String fieldname = fields.next();
				assertEquals("invoice", fieldname);
				assertEquals("'invoice' Eine Rechnungsnummer muss zwingend vorhanden sein.", next.get(fieldname).asText());
			}
		}
	}

	@Test
	public void testWithoutIban() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", "12345");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode resultNode = mapper.readTree(result.toString());
		assertEquals(ArrayNode.class, resultNode.getClass());
		ArrayNode arrayNode = ArrayNode.class.cast(resultNode);
		assertEquals(1, arrayNode.size());
		Iterator<JsonNode> entries = arrayNode.elements();
		while (entries.hasNext())
		{
			ObjectNode next = ObjectNode.class.cast(entries.next());
			Iterator<String> fields = next.fieldNames();
			while (fields.hasNext())
			{
				String fieldname = fields.next();
				assertEquals("12345", fieldname);
				assertEquals("'iban' muss die QRIban des Rechnungstellers enthalten.", next.get(fieldname).asText());
			}
		}
	}

	@Test
	public void testDocumentToAppendToDoesNotExist() throws JsonMappingException, JsonProcessingException
	{
		String nonExistentPath = (System.getProperty("user.home") + File.separator + UUID.randomUUID() + ".pdf").replace('\\', '/');
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", nonExistentPath);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode resultNode = mapper.readTree(result.toString());
		assertEquals(ArrayNode.class, resultNode.getClass());
		ArrayNode arrayNode = ArrayNode.class.cast(resultNode);
		assertEquals(1, arrayNode.size());
		Iterator<JsonNode> entries = arrayNode.elements();
		while (entries.hasNext())
		{
			ObjectNode next = ObjectNode.class.cast(entries.next());
			Iterator<String> fields = next.fieldNames();
			while (fields.hasNext())
			{
				String fieldname = fields.next();
				assertEquals("10456", fieldname);
				assertEquals("Die Quelldatei existiert nicht. Sie muss für die Verarbeitung vorhanden sein.", next.get(fieldname).asText());
			}
		}
	}
	
	@Test
	public void testWithBillAsPng() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PNG.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result);	
	}
	
	@Test
	public void testWithBillAsPngAlone() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PNG.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result);	
	}
	
	@Test
	public void testWithoutAmount() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result);	
	}
	
	@Test
	public void testWith0Amount() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 0);
		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result);	
	}
	
	@Test
	public void testQRBillOnly() throws JsonMappingException, JsonProcessingException
	{
		String out = this.output;
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
		{
			this.output = "C:\\Users\\christian\\QRBill.pdf";
			path.put("output", this.output);
		}
		else
		{
			this.output = "/Festplatte/Users/christian/QRBill.pdf";
			path.put("output", this.output);
		}
		this.output = out;
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_ONLY.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result);
	}
	
	@Test
	public void testQRBillWithFileMakerPath() throws JsonMappingException, JsonProcessingException
	{
		String out = this.output;
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
		{
			this.output = "/C:/Users/christian/QRBill.pdf";
			path.put("output", this.output);
		}
		else
		{
			this.output = "/Festplatte/Users/christian/QRBill.pdf";
			path.put("output", this.output);
		}
		this.output = out;
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_ONLY.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result);	
	}
	
	@Test
	public void testQRBillWithFileMakerPathAndInvoice() throws JsonMappingException, JsonProcessingException
	{
		String out = this.output;
		String inv = this.invoice;
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
		{
			this.output = "/C:/Users/christian/Documents/QRBill.pdf";
			path.put("output", this.output);
			this.invoice = "/C:/Users/christian/Documents/invoice.pdf";
			path.put("invoice", this.invoice);
		}
		else
		{
			this.output = "/Festplatte/Users/christian/Documents/QRBill.pdf";
			path.put("output", this.output);
			this.invoice = "/Festplatte/Users/christian/Documents/invoice.pdf";
			path.put("invoice", this.invoice);
		}
		this.output = out;
		this.invoice = inv;
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_ONLY.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result);	
	}
	
	@Test
	public void testQRBillWithFilePath() throws JsonMappingException, JsonProcessingException
	{
		String out = this.output;
		if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
		{
			this.output = "/C:/Users/christian/Documents/QRBill.pdf";
		}
		if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0)
		{
			this.output = "/Festplatte/Users/christian/Documents/QRBill.pdf";
		}
		this.output = out;
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_ONLY.name());
		form.put("graphics_format", GraphicsFormat.PNG.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result.toString());	
	}
	
	@Test
	public void testQRBillWithMissingCurrency() throws JsonMappingException, JsonProcessingException
	{
		String out = this.output;
		this.output = (System.getProperty("java.io.tmpdir") + UUID.randomUUID() + ".png");
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		this.output = out;
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_ONLY.name());
		form.put("graphics_format", GraphicsFormat.PNG.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
//		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode resultNode = mapper.readTree(result.toString());
		assertEquals(ArrayNode.class, resultNode.getClass());
		ArrayNode arrayNode = ArrayNode.class.cast(resultNode);
		assertEquals(1, arrayNode.size());
		Iterator<JsonNode> entries = arrayNode.elements();
		while (entries.hasNext())
		{
			ObjectNode next = ObjectNode.class.cast(entries.next());
			Iterator<String> fields = next.fieldNames();
			while (fields.hasNext())
			{
				String fieldname = fields.next();
				assertEquals("10456", fieldname);
				assertEquals("'currency' muss eine gültige Währung im ISO 4217 Format (3 Buchstaben) sein.", next.get(fieldname).asText());
			}
		}
	}
	
}
