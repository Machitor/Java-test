package org.machi.javatest;

import org.eclipse.milo.opcua.sdk.client.AddressSpace;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.client.subscriptions.ManagedDataItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.ManagedSubscription;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class OpcClient {

    public static void main(String[] args) throws UaException {
        // Создание клиента
        OpcUaClient client = OpcUaClient.create("opc.tcp://localhost:62640");
        try {


            client.connect().get();

            ManagedSubscription subscription = ManagedSubscription.create(client);
            AddressSpace addressSpace = client.getAddressSpace();

            // Получение узла Realtimedata
            UaNode realTimeDataNode = addressSpace.getNode(new NodeId(2, "Realtimedata"));

                List<? extends UaNode> nodes = addressSpace.browseNodes(realTimeDataNode);
                HashMap<String, String> tagValueMap = new HashMap<>();
                for (UaNode node : nodes) {
                    UaVariableNode variableNode = (UaVariableNode) addressSpace.getNode(node.getNodeId());

                    String nodeId = node.getNodeId().getIdentifier().toString();
                    String nodeType = node.getNodeId().getType().toString();
                    String value = variableNode.readValue().getValue().getValue().toString();

                    // Вывод информации о каждом дочернем узле


                    tagValueMap.put(nodeId,value);
                    System.out.println("NodeId: " + nodeId);
                    System.out.println("NodeType: " + nodeType);
                    System.out.println("Value: " +value );
                    System.out.println("----------");
                    addSub(subscription,(UaVariableNode) node);
                }



                updateTagsUaTxt(tagValueMap);


                while (true)TimeUnit.SECONDS.sleep(1);

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // Закрытие соединения
            try {
                client.disconnect().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
    private static void addSub(ManagedSubscription subscription, UaVariableNode node) throws UaException {
        ManagedDataItem dataItem = subscription.createDataItem(node.getNodeId());
        dataItem.addDataValueListener((item, value) -> {
            String nodeId = item.getNodeId().getIdentifier().toString();
            String val = value.getValue().getValue().toString();
            System.out.println("Data changed on NodeId: " + nodeId );
            updateTagValueInTxt(nodeId,val);
        });
        System.out.println(node.getNodeId().getIdentifier().toString() + " SUBBED");
    }
    private static void updateTagsUaTxt(HashMap<String, String> tags) {
        try (FileWriter writer = new FileWriter("tagsUa.txt")) {
            for (Map.Entry<String,String> tag : tags.entrySet()) {
                writer.write(tag.getKey() + " | " + tag.getValue() + "\n");
            }
            System.out.println("Tag values in "+ "tagsUa.txt" +" have been updated ");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void updateTagValueInTxt(String tagKey, String newValue) {
        File file = new File("tagsUa.txt");
        File tempFile = new File("tagsUa_temp.txt");

        try (BufferedReader reader = new BufferedReader(new FileReader(file));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            String line;
            boolean tagUpdated = false;

            // Чтение файла и запись изменений в временный файл
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(tagKey + " | ")) {
                    writer.write(tagKey + " | " + newValue + "\n");
                    tagUpdated = true;
                } else {
                    writer.write(line + "\n");
                }
            }

            // Если тег не найден, добавляем его
            if (!tagUpdated) {
                writer.write(tagKey + " | " + newValue + "\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Замена оригинального файла временным
        if (!file.delete()) {
            System.out.println("Could not delete original file.");
            return;
        }
        if (!tempFile.renameTo(file)) {
            System.out.println("Could not rename temporary file.");
        }
    }
}
