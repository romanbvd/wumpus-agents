import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPAException;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import aima.core.environment.wumpusworld.HybridWumpusAgent;
import aima.core.environment.wumpusworld.WumpusCave;
import aima.core.environment.wumpusworld.WumpusEnvironment;
import aima.core.environment.wumpusworld.WumpusPercept;
import aima.core.environment.wumpusworld.WumpusAction;
import aima.core.environment.wumpusworld.AgentPosition;


public class Environment extends Agent {
    private WumpusCave cave;
    private WumpusEnvironment env;
    private HybridWumpusAgent agent;
    private WumpusPercept percept;

    // Put agent initializations here
    protected void setup() {
        System.out.println("Environment loaded");        

        registerInDF();
        createEnv();
        
    }

    private void createEnv() {
        cave = new WumpusCave();
        env = new WumpusEnvironment(cave);
        agent = new HybridWumpusAgent();
        env.addAgent(agent);

        addBehaviour(new OnNavigatorRequests());
        addBehaviour(new OnSpeliologRequests());
        // percept = env.getPerceptSeenBy(agent);
        // System.out.println(percept.toString());

        // env.execute(agent, WumpusAction.FORWARD);

        // percept = env.getPerceptSeenBy(agent);
        // System.out.println(percept.toString());

        
    }

    private class OnNavigatorRequests extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchConversationId("navigation-environment"));

            ACLMessage msg = myAgent.receive(mt);
          if (msg != null) {
            // REQUEST Message received. Process it
            String content = msg.getContent();
            ACLMessage reply = msg.createReply();

            System.out.println("Environment: From navigator request:");
            System.out.println("Environment: " + content);

            if(content.equals("percept")) {
                percept = env.getPerceptSeenBy(agent);

                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(percept.toString());
                myAgent.send(reply);
            } else if (content.equals("coordinates")) {
                    AgentPosition position = env.getAgentPosition(agent);
    
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(position.toString());
                    myAgent.send(reply);
            }
          }
            else {
                block();
            }
        }
      }

      private class OnSpeliologRequests extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                MessageTemplate.MatchConversationId("speleolog"));

            ACLMessage msg = myAgent.receive(mt);
          if (msg != null) {
            // REQUEST Message received. Process it
            String content = msg.getContent();
            ACLMessage reply = msg.createReply();

            // System.out.println("Environment: From speliolog request:");
            // System.out.println("Environment: " + content);
            
            switch (content) {
                case  ("Forward"):
                    env.execute(agent, WumpusAction.FORWARD);
                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    break;
                case ("TurnLeft"):
                    env.execute(agent, WumpusAction.TURN_LEFT);
                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    break;
                case ("TurnRight"):
                    env.execute(agent, WumpusAction.TURN_RIGHT);
                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    break;
                case ("Grab"):
                    env.execute(agent, WumpusAction.GRAB);
                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    break;
                case ("Shoot"):
                    env.execute(agent, WumpusAction.SHOOT);
                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    break;
                case ("Climb"):
                    env.execute(agent, WumpusAction.CLIMB);
                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    break;
                default:
                    reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    break;
            }

            
            myAgent.send(reply);
          }
            else {
                block();
            }
        }
      }

    private void registerInDF() {
        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("wumpus-environment");
        sd.setName("Wumpus Environment");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
}