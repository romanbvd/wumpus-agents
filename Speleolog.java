// sudo lsof -i -P -n
// javac -d res -classpath ./jade.jar -sourcepath ./pak2/src/ Speleolog.java 
// javac -d res -classpath ./jade.jar -sourcepath ./pak2/src/ Navigator.java 
// javac -d res -classpath ./jade.jar -sourcepath ./pak2/src/ Environment.java
// java -classpath ./jade.jar:. jade.Boot -gui -agents environment:Environment\;speleolog:Speleolog\;navigator:Navigator

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class Speleolog extends Agent {
    private AID[] envAgents;
    private AID[] navAgents;

    private String command = "";

    protected void setup() {
        System.out.println("Speleolog loaded");

        addBehaviour(new TickerBehaviour(this, 10000) {
            protected void onTick() {
                System.out.println("Speleolog checks envs");

                DFAgentDescription template1 = new DFAgentDescription();
                ServiceDescription sd1 = new ServiceDescription();
                sd1.setType("wumpus-environment");
                template1.addServices(sd1);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template1);
                    System.out.println("Found the following environments agents:");
                    envAgents = new AID[result.length];
                    for (int i = 0; i < result.length; ++i) {
                        envAgents[i] = result[i].getName();
                        System.out.println(envAgents[i].getName());
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }

                DFAgentDescription template2 = new DFAgentDescription();
                ServiceDescription sd2 = new ServiceDescription();
                sd2.setType("wumpus-navigator");
                template2.addServices(sd2);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template2);
                    System.out.println("Found the following environments agents:");
                    navAgents = new AID[result.length];
                    for (int i = 0; i < result.length; ++i) {
                        navAgents[i] = result[i].getName();
                        System.out.println(navAgents[i].getName());
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }

                if(command.equals("percept") || command.equals("coordinates")) {
                    myAgent.addBehaviour(new PerceptPerformer());
                } else if(!command.equals("")) {
                    myAgent.addBehaviour(new CommandPerformer());
                }
            }
        });

        addBehaviour(new CommandsListener());
    }
    
    private class CommandsListener extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                command = msg.getContent();

                System.out.println("Speleolog: Command: " + command);
            }
            else {
                block();
            }
        }
      }


    private class CommandPerformer extends Behaviour {
        private boolean responceReceived = false;
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;

        public void action() {
            System.out.println("Speleolog: Command Action step");
            System.out.println("Speleolog: " + step);
            switch (step) {
                case 0:
                    // Send the cfp to all sellers
                    ACLMessage cfp = new ACLMessage(ACLMessage.PROPOSE);
                    for (int i = 0; i < envAgents.length; ++i) {
                        cfp.addReceiver(envAgents[i]);
                    }
                    cfp.setContent(command);
                    command = "";
                    cfp.setConversationId("speleolog");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);

                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("speleolog"),
                                         MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    ACLMessage reply = myAgent.receive();
                    System.out.println("Speleolog: Reply=================");
                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                            System.out.println("Speleolog: Proposal accepted");

                            responceReceived = true;
                        } else if(reply.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                            System.out.println("Speleolog: Proposal rejected");

                            responceReceived = true;
                        }                
                    } else {
                        System.out.println("Speleolog: Blocked behaviour");
                        block();
                    }

                    break;
            }
        }

        

        public boolean done() {
            if (step == 1 && !responceReceived) {
                System.out.println("Speleolog: Behaviour not finished");
            } else {
                System.out.println("Speleolog: Behaviour FINISHED");
            }

            System.out.println("Here " + (step == 1 && responceReceived));

            
            return (step == 1 && responceReceived);
        }
    }

    private class PerceptPerformer extends Behaviour {
        private boolean responceReceived = false;
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;

        public void action() {
            System.out.println("Speleolog: Perceptor Action step");
            System.out.println("Speleolog: " + step);
            switch (step) {
                case 0:
                    // Send the cfp to all sellers
                    ACLMessage cfp = new ACLMessage(ACLMessage.REQUEST);
                    for (int i = 0; i < navAgents.length; ++i) {
                        cfp.addReceiver(navAgents[i]);
                    }
                    cfp.setContent(command);
                    command = "";
                    cfp.setConversationId("speleolog-navigator");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);

                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("speleolog-navigator"),
                                         MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    ACLMessage msg = myAgent.receive();
                    if (msg != null) {
                        // Reply received
                        if (msg.getPerformative() == ACLMessage.INFORM) {

                            System.out.println("Speleolog: Reply from Navigator\n===================================");
                            System.out.println(msg.getContent());
                            System.out.println("================================================");

                            responceReceived = true;
                        }              
                    } else {
                        System.out.println("Speleolog: Blocked behaviour");
                        block();
                    }

                    break;
            }
        }

        

        public boolean done() {
            if (step == 1 && !responceReceived) {
                System.out.println("Speleolog: Behaviour not finished");
            } else {
                System.out.println("Speleolog: Behaviour FINISHED");
            }

            System.out.println("Speleolog: Here " + (step == 1 && responceReceived));

            
            return (step == 1 && responceReceived);
        }
    }
}
