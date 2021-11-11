package ch.eugster.swissqrbill;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.codecrete.qrbill.canvas.PDFCanvas;
import net.codecrete.qrbill.generator.Address;
import net.codecrete.qrbill.generator.Bill;
import net.codecrete.qrbill.generator.BillFormat;
import net.codecrete.qrbill.generator.GraphicsFormat;
import net.codecrete.qrbill.generator.Language;
import net.codecrete.qrbill.generator.OutputSize;
import net.codecrete.qrbill.generator.QRBill;
import net.codecrete.qrbill.generator.ValidationResult;

public class SwissQRBillGenerator 
{
	public Object generate(String json) 
	{
		// convert JSON string to Map
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode result = mapper.createArrayNode();
		JsonNode node = null;
		try 
		{
			node = mapper.readTree(json);
			if (node == null)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put("Parameter", "Der übergebene Parameter konnte nicht gelesen werden. Handelt es sich um ein Json Objekt?");
				result.add(msg);
			}
		} 
		catch (IllegalArgumentException e) 
		{
			ObjectNode msg = mapper.createObjectNode();
			msg.put("Parameter", "Der übergebene Parameter enthält ein ungültiges Element (" + e.getLocalizedMessage() + ").");
			result.add(msg);
		} 
		catch (JsonMappingException e) 
		{
			ObjectNode msg = mapper.createObjectNode();
			msg.put("Parameter", "Der übergebene Parameter konnte nicht als JSON Object aufgebaut werden (" + e.getLocalizedMessage() + "). Handelt es sich um ein gültiges Json Objekt?");
			result.add(msg);
		} 
		catch (JsonProcessingException e) 
		{
			ObjectNode msg= mapper.createObjectNode();
			msg.put("Parameter", "Der übergebene Parameter konnte nicht verarbeitet werden (" + e.getLocalizedMessage() + "). Handelt es sich um ein Json Objekt?");
			result.add(msg);
		}

		if (node != null)
		{
			String id = "Ohne Rechnungsnummer";
			try
			{
				id = node.get("invoice").asText();
			}
			catch (NullPointerException e)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put("Rechnungsnummer", "'invoice' Eine Rechnungsnummer muss zwingend vorhanden sein.");
				result.add(msg);
			}

			String path = node.get("path").get("output").asText();
			Path output = null;
			try
			{
				output = adaptFilePathname(path, mapper, result);
			}
			catch (Exception e)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put(id, "Der Pfad für die generierte Daten muss gültig sein (Systempfad oder URI).");
				result.add(msg);
			}

			BillFormat format = new BillFormat();
			format.setLanguage(guessLanguage(node.get("form")));
			try
			{
				format.setFontFamily("Arial");
				format.setGraphicsFormat(selectGraphicsFormat(node.get("form")));
			}
			catch (IllegalArgumentException e)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put("form.graphics_format", buildGraphicsFormatErrorMessage());
				result.add(msg);
			}
			try
			{
				format.setOutputSize(selectOutputSize(node.get("form")));
			}
			catch (IllegalArgumentException e)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put("form.output_size", buildOutputSizeErrorMessage());
				result.add(msg);
			}

			// Setup bill
			Bill bill = new Bill();
			bill.setFormat(format);
			if (node.get("amount") != null && node.get("amount").asDouble() > 0D)
			{
				bill.setAmountFromDouble(Double.valueOf(node.get("amount").asDouble()));
			}
			try
			{
				bill.setCurrency(node.get("currency").asText());
			}
			catch (NullPointerException e)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put(id, "'currency' muss eine gültige Währung im ISO 4217 Format (3 Buchstaben) sein.");
				result.add(msg);
			}
			bill.setReferenceType(Bill.REFERENCE_TYPE_NO_REF);
	
			// Set creditor
			Address creditor = new Address();
			try
			{
				creditor.setName(node.get("creditor").get("name").asText());
			}
			catch (NullPointerException e)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put(id, "'creditor.name' muss den Namen des Rechnungstellers enthalten (maximal 70 Buchstaben).");
				result.add(msg);
			}
			try
			{
				creditor.setAddressLine1(node.get("creditor").get("address").asText());
			}
			catch (NullPointerException e)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put(id, "'creditor.address' muss die Adresse des Rechnungstellers enthalten (maximal 70 Buchstaben).");
				result.add(msg);
			}
			try
			{
				creditor.setAddressLine2(node.get("creditor").get("city").asText());
			}
			catch (NullPointerException e)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put(id, "'creditor.city' muss Postleitzahl und Ort des Rechnungstellers enthalten (maximal 70 Buchstaben).");
				result.add(msg);
			}
			try
			{
				creditor.setCountryCode(node.get("creditor").get("country").asText());
			}
			catch (NullPointerException e)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put(id, "'creditor.country' muss den zweistelligen Landcode gemäss ISO 3166 des Rechnungstellers enthalten.");
				result.add(msg);
			}
			bill.setCreditor(creditor);
			try
			{
				String iban = node.get("iban").asText();
				if (iban == null || iban.trim().isEmpty())
				{
					throw new NullPointerException();
				}
				bill.setAccount(iban);
			}
			catch (NullPointerException e)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put(id, "'iban' muss die QRIban des Rechnungstellers enthalten.");
				result.add(msg);
			}
	
			// more bill data
			StringBuilder reference = new StringBuilder();
			if (Objects.isNull(node.get("reference")))
			{
				if (node.get("invoice") != null)
				{
					try
					{
						reference = reference.append(new BigInteger(node.get("invoice").asText()).toString());
					}
					catch (NumberFormatException e)
					{
						// Do nothing
					}
				}
				if (node.get("debtor") != null && node.get("debtor").get("number") != null)
				{
					try
					{
						reference = reference.append(new BigInteger(node.get("debtor").get("number").asText()).toString());
					}
					catch (NumberFormatException e)
					{
						// Do nothing
					}
				}
			}
			else
			{
				try
				{
					reference = reference.append(new BigInteger(node.get("reference").asText()).toString());
				}
				catch (NumberFormatException e)
				{
					// Do nothing: reference is already initialized with ""
				}
			}
			if (reference.length() == 27)
			{
				bill.setReference(reference.toString());
			}
			else
			{
				bill.createAndSetQRReference(reference.toString());
			}
			bill.setUnstructuredMessage(node.get("message").asText());
			
			// Set creditor
			if (node.get("debtor") != null)
			{
				Address debtor = new Address();
				try
				{
					debtor.setName(node.get("debtor").get("name").asText());
				}
				catch (NullPointerException e)
				{
					ObjectNode msg = mapper.createObjectNode();
					msg.put(id, "'debtor.name' muss den Namen des Rechnungempfängers enthalten (maximal 70 Buchstaben).");
					result.add(msg);
				}
				try
				{
					debtor.setAddressLine1(node.get("debtor").get("address").asText());
				}
				catch (NullPointerException e)
				{
					ObjectNode msg = mapper.createObjectNode();
					msg.put(id, "'debtor.address' muss die Adresse des Rechnungsempfängers enthalten (maximal 70 Buchstaben).");
					result.add(msg);
				}
				try
				{
					debtor.setAddressLine2(node.get("debtor").get("city").asText());
				}
				catch (NullPointerException e)
				{
					ObjectNode msg = mapper.createObjectNode();
					msg.put(id, "'debtor.city' muss Postleitzahl und Ort des Rechnungsempfängers enthalten (maximal 70 Buchstaben).");
					result.add(msg);
				}
				try
				{
					debtor.setCountryCode(node.get("debtor").get("country").asText());
				}
				catch (NullPointerException e)
				{
					ObjectNode msg = mapper.createObjectNode();
					msg.put(id, "'debtor.country' muss den zweistelligen Landcode gemäss ISO 3166 des Rechnungsempfängers enthalten.");
					result.add(msg);
				}
				bill.setDebtor(debtor);
			}
	
			// Validate QR bill
			ValidationResult validation = QRBill.validate(bill);
			if (validation.isValid() && result.isEmpty())
			{
				Path invoice = null;
				if (node.get("path") != null && node.get("path").get("invoice") != null)
				{
					path = node.get("path").get("invoice").asText();
					try
					{
						invoice = adaptFilePathname(path, mapper, result);
					}
					catch (Exception e)
					{
						ObjectNode msg = mapper.createObjectNode();
						msg.put(id, "Falls die QRBill an ein bestehendes Dokument angefügt werden soll, so muss dieses Dokument bereits bestehen und einen gültigen Namen haben.");
						result.add(msg);
					}
				}
				
				if (!Objects.isNull(invoice))
				{
					if (invoice.toFile().exists())
					{
						PDFCanvas canvas = null;
						try
						{
							byte[] targetArray = null;
							InputStream is = null;
							try
							{
								is = new FileInputStream(invoice.toFile());
								targetArray = new byte[is.available()];
								is.read(targetArray);
							}
							finally
							{
								if (is != null)
								{
									is.close();
								}
							}

							canvas = new PDFCanvas(targetArray, PDFCanvas.LAST_PAGE);
							QRBill.draw(bill, canvas);
							return "OK";
						}
						catch (IOException e)
						{
							ObjectNode msg = mapper.createObjectNode();
							msg.put(id, "Das Dokument, an das die QRBill angehängt werden soll, konnte nicht geöffnet werden.");
							result.add(msg);
						}
						finally
						{
							if (canvas != null)
							{
								try
								{
									canvas.saveAs(output);
									canvas.close();
								}
								catch (IOException e)
								{
									ObjectNode msg = mapper.createObjectNode();
									msg.put(id, "Die Zieldatei '" + invoice.toString() + "' kann nicht gespeichert werden.");
									result.add(msg);
								}
							}
						}
					}
					else
					{
						ObjectNode msg = mapper.createObjectNode();
						msg.put(id, "Die Quelldatei existiert nicht. Sie muss für die Verarbeitung vorhanden sein.");
						result.add(msg);
					}
				}
				else
				{
					// Generate QR bill
					byte[] bytes = QRBill.generate(bill);
					try 
					{
						if (output.toFile().exists())
						{
							output.toFile().delete();
						}
						OutputStream os = null;
						try
						{
							os = new FileOutputStream(output.toFile());
							os.write(bytes);
						}
						finally
						{
							if (os != null)
							{
								os.flush();
								os.close();
							}
						}
						return "OK";
					} 
					catch (FileNotFoundException e) 
					{
						ObjectNode msg = mapper.createObjectNode();
						msg.put(id, "Die Zieldatei kann nicht gefunden werden.");
						result.add(msg);
					} 
					catch (IOException e) 
					{
						e.printStackTrace();
						ObjectNode msg = mapper.createObjectNode();
						msg.put(id, "Beim Zugriff auf die Zieldatei '" + output.toString() + "' ist ein Fehler aufgetreten.");
						result.add(msg);
					} 
				}
			}
		}
		return result.toString();
	}
	
	private Language guessLanguage(JsonNode node)
	{
		JsonNode languageNode = node.get("language");
		if (languageNode != null && languageNode.asText() != null && !languageNode.asText().trim().isEmpty())
		{
			String[] availableLanguages = new String[] { "EN", "DE", "FR", "IT" };
			for (String availableLanguage : availableLanguages)
			{
				if (availableLanguage.equals(languageNode.asText()))
				{
					return Language.valueOf(availableLanguage);
				}
			}
		}
		if (Locale.getDefault().getLanguage().equals(Locale.GERMAN.getLanguage()))
		{
			return Language.DE;
		}
		else if (Locale.getDefault().getLanguage().equals(Locale.FRENCH.getLanguage()))
		{
			return Language.FR;
		}
		else if (Locale.getDefault().getLanguage().equals(Locale.ITALIAN.getLanguage()))
		{
			return Language.IT;
		}
		else
		{
			return Language.EN;
		}
	}
	
	private OutputSize selectOutputSize(JsonNode node) throws IllegalArgumentException
	{
		OutputSize outputSize = null;
		JsonNode size = node.get("output_size");
		if (size == null || size.asText() == null || size.asText().trim().isEmpty())
		{
			if (node.get("path").get("invoice") == null)
			{
				outputSize = OutputSize.A4_PORTRAIT_SHEET;
			}
			else
			{
				outputSize = OutputSize.QR_BILL_EXTRA_SPACE;
			}
		}
		try
		{
			outputSize = OutputSize.valueOf(size.asText());
		}
		catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException(buildOutputSizeErrorMessage());
		}
		return outputSize;
	}

	private GraphicsFormat selectGraphicsFormat(JsonNode node) throws IllegalArgumentException
	{
		JsonNode graphicsFormat = node.get("graphics_format");
		if (graphicsFormat == null || graphicsFormat.asText() == null || graphicsFormat.asText().trim().isEmpty())
		{
			throw new IllegalArgumentException(buildGraphicsFormatErrorMessage());
		}
		GraphicsFormat format = null;
		try
		{
			format = GraphicsFormat.valueOf(graphicsFormat.asText());
		}
		catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException(buildGraphicsFormatErrorMessage());
		}
		if (format == null || format.getClass().isAnnotationPresent(Deprecated.class))
		{
			throw new IllegalArgumentException(buildGraphicsFormatErrorMessage());
		}
		return format;
	}
	
	private String buildOutputSizeErrorMessage()
	{
		StringBuilder builder = new StringBuilder();
		for (OutputSize size : OutputSize.values())
		{
			builder = builder.append(size.name() + ", ");
		}
		String values = builder.toString().trim();
		values = values.substring(0, values.length() - 1);
		return "'output_size' muss eines der folgenden Werte sein: " + values;
	}
	
	private String buildGraphicsFormatErrorMessage()
	{
		StringBuilder builder = new StringBuilder();
		for (GraphicsFormat format : GraphicsFormat.values())
		{
			builder = builder.append(format.name() + ", ");
		}
		String values = builder.toString().trim();
		values = values.substring(0, values.length() - 1);
		return "'graphics_format' muss eines der folgenden Werte sein: " + values;
	}
	
	private Path adaptFilePathname(String path, ObjectMapper mapper, ArrayNode result) throws Exception
	{
		Path correctedPath = Objects.isNull(path) ? null : (path.trim().isEmpty() ? null : Paths.get(path));
		if (!Objects.isNull(correctedPath))
		{
			if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
			{
				if (new File(path).isAbsolute() && path.startsWith("/"))
				{
					correctedPath = Paths.get(path.substring(1));
				}
			}
			else if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0)
			{
				correctedPath = Paths.get(path);
				if (correctedPath.isAbsolute() && !correctedPath.toFile().getAbsolutePath().startsWith("/Users") && !correctedPath.toFile().getAbsolutePath().startsWith("/Library"))
				{
					correctedPath = Paths.get("/", correctedPath.subpath(1, correctedPath.getNameCount()).toString());
				}
			}
			correctedPath.toFile().getParentFile().mkdirs();
		}
		return correctedPath;
	}
	
//	private enum Parameter
//	{
//		AMOUNT("amount", false),
//		CURRENCY("currency", true),
//		IBAN("iban", true),
//		REFERENCE("reference", true),
//		PATH("path", true),
//		FORM("form", false),
//		CREDITOR("creditor", true),
//		DEBTOR("debtor", false);
//		
//		private String key;
//		
//		private boolean mandatory;
//		
//		private Parameter(String key, boolean mandatory)
//		{
//			this.key = key;
//			this.mandatory = mandatory;
//		}
//	}
//	
//	private enum Creditor
//	{
//		NAME("name", true), 
//		ADDRESS("address", true), 
//		CITY("city", true), 
//		COUNTRY("country", true);
//		
//		private String key;
//		
//		private boolean mandatory;
//		
//		private Creditor(String key, boolean mandatory)
//		{
//			this.key = key;
//			this.mandatory = mandatory;
//		}
//	}
//
//	private enum Debtor
//	{
//		NAME("name", true), 
//		ADDRESS("address", true), 
//		CITY("city", true), 
//		COUNTRY("country", true);
//		
//		private String key;
//		
//		private boolean mandatory;
//		
//		private Debtor(String key, boolean mandatory)
//		{
//			this.key = key;
//			this.mandatory = mandatory;
//		}
//	}
//	
//	private enum Form
//	{
//		GRAPHICS_FORMAT("form.graphics_format", false),
//		OUTPUT_SIZE("form.output_size", false),
//		LANGUAGE("form.language", false);
//		
//		private String key;
//		
//		private boolean mandatory;
//		
//		private Form(String key, boolean mandatory)
//		{
//			this.key = key;
//			this.mandatory = mandatory;
//		}
//	}
//
//	private enum Path
//	{
//		OUTPUT("path.output", true), 
//		INVOICE("path.invoice", false);
//		
//		private String key;
//		
//		private boolean mandatory;
//		
//		private Path(String key, boolean mandatory)
//		{
//			this.key = key;
//			this.mandatory = mandatory;
//		}
//	}
}