require 'bundler'
Bundler.require
require 'csv'

def create_csv
  properties = ["cc", "phone", "email", "ip"]
  File.open("transactions.csv", "w") do |file|
    file.puts properties.join(",")
  end
  
  transactions = File.open("transactions.csv", "a")
  50000.times do |t|
    values = [rand.to_s[2..8], Faker::PhoneNumber.short_phone_number, Faker::Internet.email, Faker::Internet.ip_v4_address]
    transactions.puts values.join(",")
    if (t%100 == 0)
      rand(1..10).times do
        # Select 1, 2 or 3 fields to change
        change = [0,1,2,3].sample(rand(1..3))
        newvalues = [rand.to_s[2..8], Faker::PhoneNumber.short_phone_number, Faker::Internet.email, Faker::Internet.ip_v4_address]
        change.each do |c|
          values[c] = newvalues[c]
        end
        transactions.puts values.join(",")
      end
    end
  end
  
  transactions.close
end

def create_graph
  create_nodes
  create_relationships
end

def create_nodes
  properties = ["cc:string:ccs", "phone:string:phones", "email:string:emails", "ip:string:ips"]

  File.open("ccs.csv", "w") do |file|
    file.puts properties[0]
  end
  File.open("phones.csv", "w") do |file|
    file.puts properties[1]
  end
  File.open("emails.csv", "w") do |file|
    file.puts properties[2]
  end
  File.open("ips.csv", "w") do |file|
    file.puts properties[3]
  end
  
  ccs = File.open("ccs.csv", "a")
  phones = File.open("phones.csv", "a")
  emails = File.open("emails.csv", "a")
  ips = File.open("ips.csv", "a")
      
  CSV.foreach('transactions.csv', :headers => true) do |row|
    ccs.puts row[0]
    phones.puts row[1]
    emails.puts row[2]
    ips.puts row[3]
  end

  %x[awk ' !x[$0]++' ccs.csv > ccs_unique.csv]
  %x[awk ' !x[$0]++' phones.csv > phones_unique.csv]
  %x[awk ' !x[$0]++' emails.csv > emails_unique.csv]
  %x[awk ' !x[$0]++' ips.csv > ips_unique.csv]        
end

def create_relationships
  File.open("ccs_to_phones.csv", "w") do |file|
    file.puts ["cc:string:ccs", "phone:string:phones", "type"].join("\t")
  end
  File.open("ccs_to_emails.csv", "w") do |file|
    file.puts ["cc:string:ccs", "email:string:emails", "type"].join("\t")
  end
  File.open("ccs_to_ips.csv", "w") do |file|
    file.puts ["cc:string:ccs", "ip:string:ips", "type"].join("\t")
  end
  File.open("phones_to_emails.csv", "w") do |file|
    file.puts ["phone:string:phones", "email:string:emails", "type"].join("\t")
  end
  File.open("phones_to_ips.csv", "w") do |file|
    file.puts ["phone:string:phones", "ip:string:ips", "type"].join("\t")
  end
  File.open("emails_to_ips.csv", "w") do |file|
    file.puts ["email:string:emails", "ip:string:ips", "type"].join("\t")
  end

  ccs_to_phones = File.open("ccs_to_phones.csv", "a")
  ccs_to_emails = File.open("ccs_to_emails.csv", "a")
  ccs_to_ips = File.open("ccs_to_ips.csv", "a")    
  phones_to_emails = File.open("phones_to_emails.csv", "a")
  phones_to_ips = File.open("phones_to_ips.csv", "a")
  emails_to_ips = File.open("emails_to_ips.csv", "a")

  CSV.foreach('transactions.csv', :headers => true) do |row|
    ccs_to_phones.puts [row[0], row[1], "RELATED"].join("\t")
    ccs_to_emails.puts [row[0], row[2], "RELATED"].join("\t")
    ccs_to_ips.puts [row[0], row[3], "RELATED"].join("\t")
    phones_to_emails.puts [row[1], row[2], "RELATED"].join("\t")
    phones_to_ips.puts [row[1], row[3], "RELATED"].join("\t")
    emails_to_ips.puts [row[2], row[3], "RELATED"].join("\t")
  end  

  %x[awk ' !x[$0]++' ccs_to_phones.csv > ccs_to_phones_unique.csv]
  %x[awk ' !x[$0]++' ccs_to_emails.csv > ccs_to_emails_unique.csv]
  %x[awk ' !x[$0]++' ccs_to_ips.csv > ccs_to_ips_unique.csv]
  %x[awk ' !x[$0]++' phones_to_emails.csv > phones_to_emails_unique.csv]  
  %x[awk ' !x[$0]++' phones_to_ips.csv > phones_to_ips_unique.csv]  
  %x[awk ' !x[$0]++' emails_to_ips.csv > emails_to_ips_unique.csv]      
end
  

def load_graph
  puts "Running the following:"
  command ="java -server -Xmx4G -jar batch-import-jar-with-dependencies.jar neo4j/data/graph.db" 
  puts command
  exec command    
end 