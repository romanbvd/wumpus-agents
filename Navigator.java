import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class Navigator extends Agent {
    private AID[] envAgents;
    private String requestFromAgent = "";
    ACLMessage msgFromSpeleologToProcess;
    String msgContentFromSpeleolog = "";

    protected void setup() {
        System.out.println("Navigator loaded");
        registerInDF();

        addBehaviour(new TickerBehaviour(this, 10000) {
            protected void onTick() {
                System.out.println("Navigator checks envs");
                // Update the list of seller agents
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("wumpus-environment");
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    System.out.println("Found the following environments agents:");
                    envAgents = new AID[result.length];
                    for (int i = 0; i < result.length; ++i) {
                        envAgents[i] = result[i].getName();
                        System.out.println(envAgents[i].getName());
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            }
        });

        addBehaviour(new RequestsRunner());
    }

    private class RequestsRunner extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchConversationId("speleolog-navigator"));

            ACLMessage msgFromSpeleolog = myAgent.receive(mt);
            if (msgFromSpeleolog != null) {
                msgFromSpeleologToProcess = msgFromSpeleolog;
                System.out.println("Navigator: Request Performer Started");
                msgContentFromSpeleolog = msgFromSpeleologToProcess.getContent();
                myAgent.addBehaviour(new RequestPerformer());
            } else {
                block();
            }
        }
      }
    

    private class RequestPerformer extends Behaviour {
        private int step = 0;
        private boolean responceSent = false;
        private MessageTemplate mt; // The template to receive replies
        String responceFromEnv = "";
        
        public void action() {
            System.out.println("Navigator: Action step: ");
            System.out.println("Navigator: " + step);
            switch (step) {
                case 0:
                    // Send the cfp to all sellers
                    ACLMessage cfp = new ACLMessage(ACLMessage.REQUEST);
                    for (int i = 0; i < envAgents.length; ++i) {
                        cfp.addReceiver(envAgents[i]);
                    }
                    cfp.setContent(msgContentFromSpeleolog);//"percept"
                    cfp.setConversationId("navigation-environment");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);

                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("navigation-environment"),
                                         MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    ACLMessage reply = myAgent.receive();
                    if (reply != null) {
                        System.out.println("Navigator: Reply++++++++++++++++++++++++++");
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            // This is an offer
                            responceFromEnv = reply.getContent();
                            System.out.println("Navigator: Reply from Env");
                            System.out.println("Navigator:" + responceFromEnv);

                        }                        
                    } else {
                        System.out.println("Navigator: Blocked behaviour");
                        block();
                    }

                    step = 2;
                    break;
                case 2:
                    ACLMessage replyToSpeleolog = msgFromSpeleologToProcess.createReply();
                    replyToSpeleolog.setPerformative(ACLMessage.INFORM);
                    replyToSpeleolog.setContent(responceFromEnv);
                    myAgent.send(replyToSpeleolog);

                    responceSent = true;
                    break;
            }
        }

        public boolean done() {
            if (step == 2 && responceSent) {
                System.out.println("Navigator: Behaviour FINISHED");
            } else {
                System.out.println("Navigator: Behaviour not finished");
            }

            System.out.println("Navigator: Here " + (step == 2 && responceSent) + "++++++++++++++++++++++++++++++++");

            
            return (step == 2 && responceSent);
        }
    }

    private void registerInDF() {
        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("wumpus-navigator");
        sd.setName("Wumpus Navigator");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
}
