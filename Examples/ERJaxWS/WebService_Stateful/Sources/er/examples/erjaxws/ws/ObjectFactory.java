
package er.examples.erjaxws.ws;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the er.examples.erjaxws.ws package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _AuthenticateResponse_QNAME = new QName("http://ws.erjaxws.examples.er/", "authenticateResponse");
    private final static QName _Authenticate_QNAME = new QName("http://ws.erjaxws.examples.er/", "authenticate");
    private final static QName _TestActionResponse_QNAME = new QName("http://ws.erjaxws.examples.er/", "testActionResponse");
    private final static QName _StatefulActionException_QNAME = new QName("http://ws.erjaxws.examples.er/", "StatefulActionException");
    private final static QName _TestAction_QNAME = new QName("http://ws.erjaxws.examples.er/", "testAction");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: er.examples.erjaxws.ws
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link StatefulActionFaultInfo }
     * 
     */
    public StatefulActionFaultInfo createStatefulActionFaultInfo() {
        return new StatefulActionFaultInfo();
    }

    /**
     * Create an instance of {@link TestActionResponse }
     * 
     */
    public TestActionResponse createTestActionResponse() {
        return new TestActionResponse();
    }

    /**
     * Create an instance of {@link Authenticate }
     * 
     */
    public Authenticate createAuthenticate() {
        return new Authenticate();
    }

    /**
     * Create an instance of {@link AuthenticateResponse }
     * 
     */
    public AuthenticateResponse createAuthenticateResponse() {
        return new AuthenticateResponse();
    }

    /**
     * Create an instance of {@link TestAction }
     * 
     */
    public TestAction createTestAction() {
        return new TestAction();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AuthenticateResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.erjaxws.examples.er/", name = "authenticateResponse")
    public JAXBElement<AuthenticateResponse> createAuthenticateResponse(AuthenticateResponse value) {
        return new JAXBElement<AuthenticateResponse>(_AuthenticateResponse_QNAME, AuthenticateResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Authenticate }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.erjaxws.examples.er/", name = "authenticate")
    public JAXBElement<Authenticate> createAuthenticate(Authenticate value) {
        return new JAXBElement<Authenticate>(_Authenticate_QNAME, Authenticate.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestActionResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.erjaxws.examples.er/", name = "testActionResponse")
    public JAXBElement<TestActionResponse> createTestActionResponse(TestActionResponse value) {
        return new JAXBElement<TestActionResponse>(_TestActionResponse_QNAME, TestActionResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link StatefulActionFaultInfo }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.erjaxws.examples.er/", name = "StatefulActionException")
    public JAXBElement<StatefulActionFaultInfo> createStatefulActionException(StatefulActionFaultInfo value) {
        return new JAXBElement<StatefulActionFaultInfo>(_StatefulActionException_QNAME, StatefulActionFaultInfo.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestAction }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.erjaxws.examples.er/", name = "testAction")
    public JAXBElement<TestAction> createTestAction(TestAction value) {
        return new JAXBElement<TestAction>(_TestAction_QNAME, TestAction.class, null, value);
    }

}
