aws dynamodb create-table \
--attribute-definitions '[
  {"AttributeName": "Id", "AttributeType": "S"},
  {"AttributeName": "Email", "AttributeType": "S"}
  ]' \
--global-secondary-indexes '[
   {
      "IndexName":"Email-index",
      "KeySchema":[
         {
            "AttributeName":"Email",
            "KeyType":"HASH"
         }
      ],
      "Projection":{
         "ProjectionType":"KEYS_ONLY"
      }
   }
]' \
--table-name UserBasicInfo \
--key-schema '[{"AttributeName": "Id", "KeyType": "HASH"}]' \
--billing-mode PAY_PER_REQUEST \
--tags Key=app-name,Value=robotalife-app


