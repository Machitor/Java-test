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

            // Подключение
            client.connect().get();

            // Создание объекта подписки
            ManagedSubscription subscription = ManagedSubscription.create(client);

            // Получение узла Realtimedata из AddressSpace
            AddressSpace addressSpace = client.getAddressSpace();
            UaNode realTimeDataNode = addressSpace.getNode(new NodeId(2, "Realtimedata"));

            List<? extends UaNode> nodes = addressSpace.browseNodes(realTimeDataNode);

            //Хэш карта для хроник тегов
            HashMap<String, String> tagValueMap = new HashMap<>();

            //Запись тегов в карту
            for (UaNode node : nodes) {
                UaVariableNode variableNode = (UaVariableNode) addressSpace.getNode(node.getNodeId());

                String nodeId = node.getNodeId().getIdentifier().toString();
                String nodeType = node.getNodeId().getType().toString();
                String value = variableNode.readValue().getValue().getValue().toString();

                // Вывод информации о каждом теге
                tagValueMap.put(nodeId,value);
                System.out.println("NodeId: " + nodeId);
                System.out.println("NodeType: " + nodeType);
                System.out.println("Value: " +value );
                System.out.println("----------");

                // Подписка на каждый тег
                addSub(subscription,(UaVariableNode) node);
            }

            // Запись тегов в файл
            updateTagsUaTxt(tagValueMap);

            //keepalive
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

    // Добавление подписки на тег
    private static void addSub(ManagedSubscription subscription, UaVariableNode node) throws UaException {
        ManagedDataItem dataItem = subscription.createDataItem(node.getNodeId());
        dataItem.addDataValueListener((item, value) -> {
            String nodeId = item.getNodeId().getIdentifier().toString();
            String val = value.getValue().getValue().toString();
            System.out.println("Данные изменились на NodeId: " + nodeId );
            updateTagValueInTxt(nodeId,val);
        });
    }

    // Запись списка тегов в файл 
    private static void updateTagsUaTxt(HashMap<String, String> tags) {
        try (FileWriter writer = new FileWriter("tagsUa.txt")) {
            for (Map.Entry<String,String> tag : tags.entrySet()) {
                writer.write(tag.getKey() + " | " + tag.getValue() + "\n");
            }
            System.out.println("Значения тегов "+ "tagsUa.txt" +" были обновлены ");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Замена значения тега
    private static void updateTagValueInTxt(String tagKey, String newValue) {
        File file = new File("tagsUa.txt");
        File tempFile = new File("tagsUa_temp.txt");

        try (BufferedReader reader = new BufferedReader(new FileReader(file));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            String line;
            boolean tagUpdated = false;

            // Чтение файла и запись изменений во временный файл
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
            System.out.println("Невозможно удалить файл.");
            return;
        }
        if (!tempFile.renameTo(file)) {
            System.out.println("Невозможно переименовать временный файл.");
        }
    }
}
