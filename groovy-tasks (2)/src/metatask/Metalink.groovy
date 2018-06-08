package metatask
import org.apache.tools.ant.BuildException
import org.apache.tools.ant.Project
import org.apache.tools.ant.Task
import org.apache.tools.ant.types.FileSet
import org.apache.tools.ant.types.resources.FileResource
import org.w3c.dom.Document

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import java.nio.file.Files
import java.security.MessageDigest
import java.text.SimpleDateFormat

class Metalink extends Task{
	String url
	String file
	FileSet fileSet
	Document xml
	Project project

	@Override
	void execute() throws BuildException {
		url = url?: project.properties['server.files.url']
		xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
		def root = xml.createElementNS("urn:ietf:params:xml:ns:metalink","metalink")
		xml.appendChild(root)

		String pattern = "EEE, MMM d, ''yy"
		SimpleDateFormat formatter
		formatter = new SimpleDateFormat(pattern)
		
		def published = xml.createElement("published")
		published.appendChild(xml.createTextNode(formatter.format(new Date())))
		root.appendChild(published)

		fileSet.each { file ->
			if(!file.isDirectory()) {
				root.appendChild(addFileToXml(file))
			}
		}

		def transformer = TransformerFactory.newInstance().newTransformer()
		transformer.setOutputProperty(OutputKeys.INDENT,"yes")
		def source = new DOMSource(xml)
		def result = new StreamResult(new FileOutputStream(file))
		transformer.transform(source, result)
	}

	def addFileToXml(FileResource file){
		def element = xml.createElement("file")
		element.setAttribute("name",file.getFile().getName())

		def size = xml.createElement("size")
		size.appendChild(xml.createTextNode(file.getFile().length()+""))

		def md5 = hash(file.getFile())
		def hash = xml.createElement("hash")
		hash.appendChild(xml.createTextNode(md5))
		hash.setAttribute("type","md5")

		def url = xml.createElement("url")
		url.appendChild(xml.createTextNode(this.url+file.getFile().getName()))

		element.appendChild(hash)
		element.appendChild(size)
		element.appendChild(url)

		return element
	}

	private static String hash(File file){
		def dig = MessageDigest.getInstance("MD5").digest(Files.readAllBytes(file.toPath()))
		def sb = new StringBuilder()
		dig.each {
			sb.append(String.format(String.format("%02x", it & 0xff)))
		}
		return sb.toString()
	}

	void setFile(String file) {
		this.file = file
	}

	void addFileSet(FileSet fileSet) {
		this.fileSet = fileSet
	}

	void setProject(Project project) {
		this.project = project
	}
}